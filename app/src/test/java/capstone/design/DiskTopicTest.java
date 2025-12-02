package capstone.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import capstone.design.message.Message;
import capstone.design.message.MessageType;
import capstone.design.topic.TopicRecord;
import capstone.design.topic.disk.DiskTopic;
import io.netty.channel.FileRegion;

public class DiskTopicTest {
    
    DiskTopic topic;
    Message message;

    @BeforeEach
    void beforeEach() {
        topic = DiskTopic.of("topic");
        message = Message.builder()
            .type(MessageType.REQ_PUSH)
            .topicName("topic")
            .clientId("tester2")
            .partition("1")
            .payload("payload")
            .build();
    }

    @AfterEach
    void afterEach() {
        // topic.clearAll();
    }

    @Test
    void pushTest() {
        topic.push(message.partition(), message);
    }

    @Test
    void peekTest() {
        TopicRecord record = topic.peek(message.partition(), message.clientId(), message);
        assertNotNull(record);

        assertInstanceOf(FileRegion.class, record.message().payload());
        assertEquals(2, record.message().offset());
        topic.commit(message.partition(), message.clientId(), record.message().offset(), message);
    }
}
