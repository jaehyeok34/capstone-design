package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.example.broker.topic.MemoryTopic;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MemoryTopicTest {

    private MemoryTopic topic;

    @BeforeAll
    void setup() { topic = new MemoryTopic(); }

    @Test
    @Order(1)
    void push() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("hello world".getBytes(StandardCharsets.UTF_8));
        
        topic.push(buf);
        assertEquals(1, topic.size());
    }

    @Test
    @Order(2)
    void pull() { 
        ByteBuf buf = topic.pull().buf();
        byte[] arr = new byte[buf.readableBytes()];
        buf.readBytes(arr);

        String msg = new String(arr, StandardCharsets.UTF_8);
        assertEquals("hello world", msg );
        assertEquals(0, topic.size());
    }
}
