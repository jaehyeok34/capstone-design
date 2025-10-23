package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import org.example.message.Message;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.topic.Topic;
import org.example.topic.TopicManager;
import org.example.topic.TopicRecord;
import org.example.topic.disk.DiskTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TopicManagerTest {

    TopicManager manager;

    @BeforeEach
    void beforeEach() {
        manager = new TopicManager(Map.of(
            "memory_topic", Topic.Type.MEMORY,
            "disk_topic", Topic.Type.DISK
        ));
    }

    @AfterEach
    void afterEach() throws Exception{ 
        if (manager.topic("disk_topic").get() instanceof DiskTopic topic) {
            topic.clearFiles(0);
            Files.deleteIfExists(topic.partitionPath(0));
            Files.deleteIfExists(topic.rootPath());
        }
    }

    @Test
    void requestPush() {
        add("memory_topic");
        assertEquals(1, manager.topic("memory_topic").get().length(0));

        add("disk_topic");
        assertEquals(1, manager.topic("disk_topic").get().length(0));
    }

    @Test
    void requestPull() {
        add("memory_topic"); // 데이터 준비
        var header1 = MessageHeader.builder(
            MessageHeader.Type.REQ_PULL, "memory_topic"
        ).partition(0)
        .build();

        var result1 = manager.process(MessageFrame.of(header1, null));
        assertNotNull(result1);
        assertNotNull(result1.header());
        assertNotNull(result1.message());

        assertEquals(MessageHeader.Type.RES_PULL, result1.header().type());
        assertEquals(header1.topicName(), result1.header().topicName());
        assertEquals(header1.partition(), result1.header().partition());
        assertInstanceOf(TopicRecord.class, result1.message());
        assertEquals("hello world".length(), ((TopicRecord) result1.message()).length());

        add("disk_topic");
        var header2 = MessageHeader.builder(
            MessageHeader.Type.REQ_PULL, "disk_topic"
        ).partition(0)
        .build();

        var result2 = manager.process(MessageFrame.of(header2, null));
        assertNotNull(result2);
        assertNotNull(result2.header());
        assertNotNull(result2.message());

        assertEquals(MessageHeader.Type.RES_PULL, result2.header().type());
        assertEquals(header2.topicName(), result2.header().topicName());
        assertEquals(header2.partition(), result2.header().partition());
        assertInstanceOf(TopicRecord.class, result2.message());
        assertEquals("hello world".length(), ((TopicRecord) result2.message()).length());
    }

    void add(String topicName) {
        Message message = Message.of("hello world".getBytes(StandardCharsets.UTF_8));
        MessageHeader header = MessageHeader.builder(
            MessageHeader.Type.REQ_PUSH, topicName
        ).partition(0)
        .messageLength(message.length())
        .build();

        manager.process(MessageFrame.of(header, message));
    }
}

