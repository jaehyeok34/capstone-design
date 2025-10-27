package capstone.design;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import capstone.design.broker.Broker;
import capstone.design.client.Consumer;
import capstone.design.client.Producer;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.spy.SpyContext;
import capstone.design.topic.Topic;
import capstone.design.topic.disk.DiskTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NetworkTest {

    Broker broker;
    ExecutorService executor;

    final String t1 = "memory_topic";
    final String t2 = "disk_topic";
    final int partition = 0;
    final int port = 3400;
    final String payload = "payload";
    final List<ByteBuf> bufs = new ArrayList<>();

    void add() throws Exception {
        for (String t : List.of(t1, t2)) {
            ByteBuf buf = Unpooled.buffer().writeBytes(payload.getBytes(StandardCharsets.UTF_8));
            bufs.add(buf);

            broker.topic(t).push(partition, "user", buf.retain());
        }
    }

    @BeforeEach
    void beforeEach() throws Exception {
        broker = Broker.builder()
            .port(port)
            .addTopic(t1, Topic.Type.MEMORY)
            .addTopic(t2, Topic.Type.DISK)
            .build();

        executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                broker.start();
            } catch (Exception ignored) {}

            broker.close();
        });

        while (!broker.isActive()) {
            Thread.sleep(100);
        } // 서버가 열릴 때까지 대기
    }

    @AfterEach
    void afterEach() throws IOException {
        broker.close();
        clear();
        executor.shutdown();
        
        for (ByteBuf buf : bufs) {
            while (buf.refCnt() > 0) {
                buf.release();
            }
        }
        bufs.clear();
    }

    @Test
    void invalidTopicProduceTest() throws Exception {
        try (Producer producer = new Producer("localhost", port)) {
            // 존재하지 않는 토픽에 데이터 전송 시도
            assertDoesNotThrow(() -> {
                // 서버에서 존재하지 않는 토픽에 데이터 전송 시 토픽 조회 단계에서 무시되고 RES_PUSH 응답이 옴
                producer.syncProduce("invalid_topic", partition, payload);
                producer.syncProduce(t1, 99, payload);
            });
        }
    }

    @Test
    void produceTest() throws Exception {
        String id = "user";
        try (Producer producer = new Producer("localhost", port, id)) {
            // 메시지 각각 2번씩 전송
            for (int i = 0; i < 2; i++) {
                producer.asyncProduce(t1, partition, payload + i);
                producer.asyncProduce(t2, partition, payload + i);
            }

            // 서버가 데이터를 처리하는 시간 최대 3초 대기
            int count = 0;
            while (broker.topic(t2).length(partition, id) == 0 && count < 10) {
                Thread.sleep(300);
                count++;
            }

            assertTrue(count < 10); // 데이터가 정상 처리 됐는지 검증

            // 두 토픽 모두 메시지가 2개인지 확인
            List.of(t1, t2).forEach(t -> assertEquals(2, broker.topic(t).length(partition, id)));
        }
    }

    @Test
    void invaildConsumeTest() throws Exception {
        try (Consumer consumer = new Consumer("localhost", port, "user")) {
            // 데이터가 없는 상태
            Message mm = consumer.consume(t1, partition); // memory topic에 데이터 요청
            Message dm = consumer.consume(t2, partition); // disk topic에 데이터 요청

            assertNull(mm.option(MessageOption.PAYLOAD));
            assertNull(dm.option(MessageOption.PAYLOAD));
        }
    }

    @Test
    void consumeTest() throws Exception {
        try (Consumer consumer = new Consumer("localhost", port, "user")) {
            add(); // 데이터 준비

            Message mm = consumer.consume(t1, partition); // memory topic에 데이터 요청
            Message dm = consumer.consume(t2, partition); // disk topic에 데이터 요청

            // 토픽 상태 검증
            assertEquals(0, broker.topic(t1).length(partition, "user"));

            // disk topic은 cursor만 증가해야 함
            assertEquals(1, broker.topic(t2).length(partition, "user")); 
            assertEquals(1, ((DiskTopic) broker.topic(t2)).cursor(partition));

            // payload 검증
            assertNotNull(mm.option(MessageOption.PAYLOAD));
            assertEquals(payload, mm.option(MessageOption.PAYLOAD, ByteBuf.class).readString(payload.length(), StandardCharsets.UTF_8));

            assertNotNull(dm.option(MessageOption.PAYLOAD));
            assertEquals(payload, dm.option(MessageOption.PAYLOAD, ByteBuf.class).readString(payload.length(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void subscribeMemoryTopicAndConsumeTest() throws Exception { 
        try (Consumer consumer = new Consumer("localhost", port, "user1");) {
            BlockingQueue<Message> out = new LinkedBlockingQueue<>();

            // 구독 과정에서 실패(예외) 하지 않아야 함
            ExecutorService executor = consumer.subscribeAndConsume(t1, partition, out);
            
            /*
             * NettyInitializer의 경우 test 환경에서 동일 핸들러(MessageDecoder) 등이 있으면
             * new 키워드를 사용하더라도 재사용하는 문제가 있어서
             * id="user2"는 별도의 consumer 객체를 만들지 않고 topic에 직접 구독하여 테스트함
             */
            SpyContext ctx = new SpyContext();
            Queue<Object> q = ctx.channel.queue;
            broker.topic(t1).subscribe(ctx, partition, "user2");

            assertEquals(0, out.size()); // 아직 데이터 없어야함

            // 데이터 추가
            ByteBuf buf = Unpooled.buffer().writeBytes(payload.getBytes(StandardCharsets.UTF_8));
            bufs.add(buf);
            broker.topic(t1).push(partition, consumer.id(), buf.retain()); // user1에만 데이터 추가

            /*
             * user2도 topic/partition을 구독했지만,
             * memory topic의 경우 id 기준으로 큐가 나눠있기 때문에
             * user2는 알람을 받지 않아야 하기 때문에 q가 비어있어야 함
             * -> ctx.write() 호출 시 SpyContext는 큐에 값을 쌓이게 되는데
             * 호출되지 않아야 하기 때문에 비어있는게 맞음
             */
            assertEquals(0, q.size());

            // consume 테스트
            Message response = out.take(); // 구독한 데이터 받기
            assertEquals(0, out.size()); // 앞에서 하나 꺼냈기 때문에 비어 있어야함

            // payload 검증
            Byte type = response.option(MessageOption.TYPE, Byte.class);
            String id = response.option(MessageOption.ID, String.class);
            String tn = response.option(MessageOption.TOPIC_NAME, String.class);
            Integer p = response.option(MessageOption.PARTITION, Integer.class);
            Long c = response.option(MessageOption.CURSOR, Long.class);
            ByteBuf pb = response.option(MessageOption.PAYLOAD, ByteBuf.class);

            assertEquals(Message.Type.RES_PULL.getByte(), type);
            assertEquals(consumer.id(), id);
            assertEquals(t1, tn);
            assertEquals(partition, p);
            assertEquals(-1, c); 
            assertEquals(payload, pb.readString(payload.length(), StandardCharsets.UTF_8));

            executor.shutdownNow();
        }
    }

    @Test
    void subscribeDiskTopicAndConsumeTest() throws Exception {
        try (Consumer consumer = new Consumer("localhost", port, "user1")) {
            BlockingQueue<Message> out = new LinkedBlockingQueue<>();

            // memory topic과 동일하게 구독 과정에서는 실패하지 않아야 함
            ExecutorService executor = consumer.subscribeAndConsume(t2, partition, out);

            /*
             * 마찬가지로 id="user"는 별도의 consumer 객체를 만들지 않고 
             * 직접 topic에 구독하여 테스트 함
             */
            SpyContext ctx = new SpyContext();
            Queue<Object> q = ctx.channel.queue;
            broker.topic(t2).subscribe(ctx, partition, "user2");

            assertEquals(0, out.size()); // 아직 데이터 없어야함

            // 데이터 추가
            ByteBuf buf = Unpooled.buffer().writeBytes(payload.getBytes(StandardCharsets.UTF_8));
            bufs.add(buf);
            broker.topic(t2).push(partition, consumer.id(), buf.retain());

            Message response = out.take(); // 구독한 데이터 받기
            assertEquals(0, out.size()); // 앞에서 하나 꺼냈기 때문에 비어 있어야함

            /*
             * 디스크 토픽의 경우 아이디와 상관없이 topic/partition을 구독하면
             * 구독한 모든 클라이언트(id)에게 알림이 가기 때문에
             * user2도 알림을 받아야 함
             */
            assertTrue(q.size() > 0);

            // payload 검증
            Byte type = response.option(MessageOption.TYPE, Byte.class);
            String id = response.option(MessageOption.ID, String.class);
            String tn = response.option(MessageOption.TOPIC_NAME, String.class);
            Integer p = response.option(MessageOption.PARTITION, Integer.class);
            Long c = response.option(MessageOption.CURSOR, Long.class);
            ByteBuf pb = response.option(MessageOption.PAYLOAD, ByteBuf.class);

            assertEquals(Message.Type.RES_PULL.getByte(), type);
            assertEquals(consumer.id(), id);
            assertEquals(t2, tn);
            assertEquals(partition, p);
            assertEquals(-1, c);
            assertEquals(payload, pb.readString(payload.length(), StandardCharsets.UTF_8));

            executor.shutdown();
        }
    }
    
    void clear() throws IOException {
        if (broker.topic(t2) instanceof DiskTopic topic) {
            topic.clearFiles(partition);
            Files.deleteIfExists(topic.partitionPath(partition));
            Files.deleteIfExists(topic.rootPath());
        }
    }
}
