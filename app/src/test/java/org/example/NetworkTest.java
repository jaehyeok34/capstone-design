package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.broker.Broker;
import org.example.client.Consumer;
import org.example.client.Producer;
import org.example.message.Message;
import org.example.topic.Topic;
import org.example.topic.disk.DiskTopic;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class NetworkTest {

    private static Broker broker;
    private static final String memory = "memory_topic_test";
    private static final String disk = "disk_topic_test";
    private static final String message = "hello world";

    @BeforeAll
    static void beforeAll() throws Exception {
        startBroker();
    }

    @AfterAll
    static void afterAll() throws Exception {
        if (broker != null) {
            if (broker.getTopic(disk) instanceof DiskTopic topic) {
                topic.getFileGroup().clearAll();
            }

            broker.close();
        }

        System.out.println("broker 종료");
    }

    @Test
    void produce() throws Exception {
        // 메시지 수신 대기 후, 잘 들어 갔는지 확인
        try (Producer producer = new Producer(1234)) {
            producer.request(memory, message);
            producer.request(disk, message);
        }

        Thread.sleep(1000);
        assertEquals(1, broker.getTopicLength(memory));
        assertEquals(1, broker.getTopicLength(disk));
    }

    @Test
    void consume() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try (Consumer consumer = new Consumer(1234)) {
                List<Message> messages = new ArrayList<>();
                messages.add(consumer.request(memory));
                messages.add(consumer.request(disk));
                
                messages.forEach(msg -> {
                    assertEquals(message.length(), msg.getLength());
                });
            } catch (Exception ignore) {}
        });
    }

    static void startBroker() throws Exception {
        broker = Broker.builder()
            .port(1234)
            .addTopic(memory, Topic.Type.MEMORY)
            .addTopic(disk, Topic.Type.DISK)
            .build();

        ExecutorService executor1 = Executors.newSingleThreadExecutor();
        executor1.submit(() -> {
            try {
                broker.start();
            } catch (Exception ignore) {}
        });
    }

    static void prepareData() throws Exception {
        ByteBuf buf1 = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);
        ByteBuf buf2 = Unpooled.copiedBuffer(message, StandardCharsets.UTF_8);
        
        NetworkTest.broker.getTopic(memory).push(buf1.retain());
        NetworkTest.broker.getTopic(disk).push(buf2.retain());
    }
}
