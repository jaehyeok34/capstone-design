package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.broker.Broker;
import org.example.client.Consumer;
import org.example.client.Producer;
import org.example.message.Message;
import org.example.topic.Topic;
import org.example.topic.disk.DiskTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NetworkTest {

    Broker broker;
    final String t1 = "memory_topic";
    final String t2 = "disk_topic";
    final int partition = 0;
    final int port = 3400;
    final String message = "message";

    @BeforeEach
    void beforeEach() throws Exception {
        broker = Broker.builder()
            .port(port)
            .addTopic(t1, Topic.Type.MEMORY)
            .addTopic(t2, Topic.Type.DISK)
            .build();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                broker.start();
            } catch (InterruptedException __) {
                broker.close();
            }
        });

        while (!broker.isActive()) {
            Thread.sleep(100);
        } // 서버가 열릴 때까지 대기
    }

    @AfterEach
    void afterEach() throws IOException {
        broker.close();
        clear();
    }

    @Test
    void produceTest() throws Exception {
        try (Producer producer = new Producer(port)) {
            // 메시지 각각 2번씩 전송
            for (int i = 0; i < 2; i++) {
                producer.request(t1, partition, message + i);
                producer.request(t2, partition, message + i);
            }

            // client -> server로 writeAndFlush 후, 실제 server가 이를 수신해서 처리하는 시간 대기
            // 단, 최대 1초 까지만 대기
            int limit = 10;
            while (broker.topic(t1).get().length(partition) == 0 && limit > 0) {
                Thread.sleep(100);
                limit--;
            }

            assertTrue(limit > 0); // 데이터가 정상 처리 됐는지 검증(limit이 0이면 토픽에 갱신 실패 했다는 의미)
            assertEquals(2, broker.topic(t1).get().length(partition));
            assertEquals(2, broker.topic(t2).get().length(partition));
        }
    }

    @Test
    void invalidProduceTest() throws Exception {
        try (Producer producer = new Producer(port)) {
            // 존재하지 않는 토픽에 데이터 전송 시도
            assertDoesNotThrow(() -> {
                producer.request("invalid_topic", partition, message);
                producer.request(t1, 99, message);
            });
        }
    }
    

    @Test
    void consumeTest() throws Exception {
        addData(); // 데이터 준비

        try (Consumer consumer = new Consumer(port)) {
            List.of(t1, t2).forEach(topic -> {
                Optional<Message> msg = consumer.request(topic, partition);
                assertTrue(msg.isPresent());

                msg.ifPresent(m -> {
                    // 꺼낸 데이터 검증
                    assertEquals(message, m.toByteBuf().toString(StandardCharsets.UTF_8));
                    assertEquals(1, m.toByteBuf().refCnt());
                });
            });
        }

        // 토픽 상태 검증
        assertEquals(0, broker.topic(t1).get().length(partition));
        assertEquals(1, ((DiskTopic) broker.topic(t2).get()).cursor(partition));
    }

    @Test
    void invalidConsumeTest() throws Exception {
        try (Consumer consumer = new Consumer(port)) {
            // 존재하지 않는 토픽에 데이터 요청
            var msg1 = consumer.request("invalid_topic", partition);
            assertTrue(msg1.isEmpty());

            // 존재하지 않는 파티션에 데이터 요청(토픽은 존재)
            var msg2 = consumer.request(t1, 99);
            assertTrue(msg2.isEmpty());

            // 데이터가 없는 파티션에 데이터 요청(토픽, 파티션 존재)
            assertEquals(0, broker.topic(t1).get().length(partition)); // 데이터 없는지 확인
            var msg3 = consumer.request(t1, partition);
            assertTrue(msg3.isEmpty());

            assertEquals(0, broker.topic(t2).get().length(partition));
            var msg4 = consumer.request(t2, partition);
            assertTrue(msg4.isEmpty());
        }
    }

    void addData() {
        List.of(t1, t2).forEach(t -> {
            broker.topic(t)
                .ifPresent(topic -> {
                    ByteBuf buf = Unpooled.buffer()
                        .writeBytes(message.getBytes(StandardCharsets.UTF_8));

                    topic.push(partition, buf.retain());
                });
        });
    }

    void clear() throws IOException {
        if (broker.topic(t2).get() instanceof DiskTopic topic) {
            topic.clearFiles(partition);
            Files.deleteIfExists(topic.partitionPath(partition));
            Files.deleteIfExists(topic.rootPath());
        }
    }
}
