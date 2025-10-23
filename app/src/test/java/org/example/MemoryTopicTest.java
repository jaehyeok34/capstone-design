package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;

import org.example.topic.memory.MemoryTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MemoryTopicTest {

    private MemoryTopic topic;
    private final int partition = 0;

    void prepareData() {
        topic.push(partition, Unpooled.copiedBuffer("message".getBytes()));
    }

    @BeforeEach
    void beforeEach() {
        topic = MemoryTopic.of();
        prepareData();
    }

    @AfterEach
    void afterEach() {
        topic = null;
    }

    @Test
    void push() {
        assertEquals(1, topic.length(partition));
    }

    @Test
    void pull() {
        topic.pull(partition)
            .ifPresent(record -> {
                if (record instanceof ByteBuf buf) {
                    String msg = buf.readString("message".length(), Charset.defaultCharset());
                    assertEquals("message", msg);
                    assertEquals(0, topic.length(partition));
                }
            });
    }
}
