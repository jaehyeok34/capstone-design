package capstone.design;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import capstone.design.broker.Broker;
import capstone.design.client.Consumer;
import capstone.design.client.Producer;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import capstone.design.topic.Topic;
import capstone.design.topic.disk.DiskTopic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MultiClientTest {
    
    Broker broker;
    ExecutorService brokerService;
    final String topicName = "multi_client_topic";
    final List<ByteBuf> bufs = new ArrayList<>();
    final String filePath = "/Users/jaehyeok34/Desktop/capstone-design/new_message_broker/option_mapping_table.properties";
    final int clientCount = 100;
    final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

    void clear() throws IOException {
        if (broker.topic(topicName) instanceof DiskTopic topic) {
            topic.clearFiles(0);
            topic.clearFiles(1);
            Files.deleteIfExists(topic.partitionPath(0));
            Files.deleteIfExists(topic.rootPath());
        }
    }
    
    @BeforeEach
    void beforeEach() throws Exception {
        broker = Broker.builder()
            .port(1234)
            .addTopic(topicName, Topic.Type.DISK)
            .build();

        brokerService = java.util.concurrent.Executors.newSingleThreadExecutor();
        brokerService.submit(() -> {
            try {
                broker.start();
            } catch (Exception ignored) {}

            broker.close();
        });

        while (!broker.isActive()) {
            Thread.sleep(100);
        } // 서버 실행까지 대기
    }

    @AfterEach
    void afterEach() throws Exception {
        brokerService.shutdownNow();
        clear();

        for (ByteBuf buf : bufs) {
            while (buf.refCnt() > 0) {
                buf.release();
            }
        }
        bufs.clear();
    }

    @Test
    void multiClientTest() throws Exception {
        List<ExecutorService> services = new ArrayList<>();
        
        // 처리자 실행
        ExecutorService processorService = Executors.newSingleThreadExecutor();
        processorService.submit(() -> {
            try {
                processor();
            } catch (Exception e) { System.err.println("처리자 중단: " + e); }
        });
        services.add(processorService);

        // 요청자 실행
        String clientId = "client_";
        for (int i = 1; i <= clientCount; i++) {
            ExecutorService clientService = Executors.newSingleThreadExecutor();
            int identity = i;
            clientService.submit(() -> {
                try {
                    worker(clientId + identity, identity);
                } catch (Exception e) { System.err.println("요청자 중단: " + e); }
            });

            services.add(clientService);
        }   

        int count = 0;
        while (true) {
            System.out.println(queue.take() + " 작업 완료(" + (++count) + "/" + clientCount + ")");
            if (count >= clientCount) {
                break;
            }
        }

        for (ExecutorService service : services) {
            service.shutdownNow();
        }
        System.out.println("모두 종료됨");
    }

    void processor() throws Exception {
        // 처리 비즈니스 로직
        try (
            Consumer consumer = new Consumer("localhost", 1234, "main", filePath);
            Producer producer = new Producer("localhost", 1234, "main", filePath)
        ) {
            // 요청 토픽(파티션) 구독
            BlockingQueue<Message> notifiedQueue = new LinkedBlockingQueue<>();
            consumer.subscribe(topicName, 0, notifiedQueue);

            while (true) {
                Message notified = notifiedQueue.take(); // TOPIC_UPDATED 대기: 요청이 들어옴
                
                // 동일한 폼으로 요청 획득용 메시지 생성 및 요청
                notified.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte());
                Message reqMsg = consumer.consume(notified);
                
                Thread.sleep(100); // 처리 비즈니스 로직(reqMsg를 활용해서 처리한다고 가정...)
                
                // 결과 토픽(파티션)에 결과 전송(폼 재사용: identity 옵션이 포함되어 있기 때문에)
                notified.removeOptions(
                    MessageOption.CURSOR, 
                    MessageOption.OFFSET, 
                    MessageOption.REMAINING_COUNT,
                    MessageOption.SUCCESS,
                    MessageOption.REQUEST_ID
                ).addOptions(Map.of(
                    MessageOption.MESSAGE_TYPE, MessageType.REQ_PUSH.getByte(),
                    MessageOption.TOPIC_NAME, topicName,
                    MessageOption.PARTITION, 1,
                    MessageOption.CLIENT_ID, "main",
                    MessageOption.PAYLOAD, reqMsg.option(MessageOption.PAYLOAD)
                ));
                
                producer.syncProduce(notified);
                System.out.println("처리 완료: " + notified.option("identity"));
            }
        }
    }

    void worker(String clientId, int identity) throws Exception {
        Thread.sleep(1000); // processor가 먼저 구독하도록 대기
        /*
         * 1. 결과 토픽(파티션) 구독
         * 2. 요청 토픽(파티션)에 메시지 전송
         * 3. 결과 토픽(파티션)에 결과 메시지가 갱신된 경우(알림 받을 경우) 내가 보낸 요청인지 확인
         * 4. 내가 보낸 요청이 아닌 경우 다시 알림 대기
         * 5. 내가 보낸 요청일 경우, TOPIC_UPDATED 메시지에 타입만 변경하여 consume 요청
         * 6. 결과 검증
         */
        try (
            Consumer consumer = new Consumer("localhost", 1234, clientId, filePath);
            Producer producer = new Producer("localhost", 1234, clientId, filePath)
        ) {
            // 1
            BlockingQueue<Message> notifiedQueue = new LinkedBlockingQueue<>();
            ExecutorService notifier = consumer.subscribe(topicName, 1, notifiedQueue);
            if (notifier == null) {
                throw new IllegalStateException("구독 실패: " + clientId);
            }

            // 메시지 생성
            ByteBuf buf = Unpooled.buffer().writeBytes(clientId.getBytes(StandardCharsets.UTF_8));
            bufs.add(buf);

            Message msg = new Message().addOptions(Map.of(
                MessageOption.MESSAGE_TYPE, MessageType.REQ_PUSH.getByte(),
                MessageOption.TOPIC_NAME, topicName,
                MessageOption.PARTITION, 0,
                MessageOption.CLIENT_ID, clientId,
                MessageOption.PAYLOAD, buf,
                "identity", identity // 커스텀 옵션
            ));

            producer.syncProduce(msg); // 2 요청 전송

            Message result = null;
            while (result == null) {
                Message notified = notifiedQueue.take(); // 3
                String id = notified.option("identity", String.class);
                if (id == null || Integer.parseInt(id) != identity) { // 내가 보낸 메시지가 아닌 경우
                    continue; // 4
                }
                
                // 5
                // TOPIC_UPDATED 메시지에 타입만 변경해서 consume 요청
                notified.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte());
                result = consumer.consume(notified);
            }

            // 6
            // 결과가 정상인지 검증
            if (result.option(MessageOption.PAYLOAD) instanceof ByteBuf payload) {
                String s = payload.readString(payload.readableBytes(), StandardCharsets.UTF_8);
                System.out.println("검증 성공: " + s);
                payload.release();
            }

            notifier.shutdownNow();
            queue.put(clientId);
        }
    }
}
