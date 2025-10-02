package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.example.broker.Broker;
import org.example.broker.topic.Topic;
import org.example.client.Producer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.netty.buffer.ByteBuf;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NetworkTest {

    private Broker b;

    @BeforeAll
    void setup() throws Exception {
        b = new Broker.Builder()
            .port(1234)
            .addTopic("t1", Topic.Type.MEMORY)
            .build();
        
        ExecutorService es = Executors.newSingleThreadExecutor();
        es.submit(() -> {
            try { b.start(); }
            catch (InterruptedException e) { e.printStackTrace(); }
        });
    }

    @Test
    void produce() throws Exception {
        try (Producer p = new Producer(1234)) {
            assertDoesNotThrow(() -> p.request("t1", "hello world"));
            Thread.sleep(1000);
            assertEquals(1, b.getTopicManager().getTopic("t1").size());
            
            ByteBuf msg = (ByteBuf) b.getTopicManager().getTopic("t1").pull().value();
            assertEquals("hello world", msg.readString("hello world".length(), StandardCharsets.UTF_8));
        }
    }
}
