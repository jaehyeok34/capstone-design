package org.example;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.example.message.Message;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.netty.client.ClientInboundHandler;
import org.example.topic.Topic;
import org.example.topic.TopicManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientInboundHandlerTest {
    
    ClientInboundHandler handler;
    TopicManager manager;
    final String topicName = "test_topic";
    final int partition = 1234;
    final Queue<CompletableFuture<MessageFrame>> requests = new ArrayDeque<>();

    @BeforeEach
    void beforeEach() {
        manager = new TopicManager(Map.of(topicName, Topic.Type.MEMORY));
        handler = new ClientInboundHandler(requests::poll);
    }   

    @Test
    void channelReadTest() throws Exception {
        Message message = Message.of("hello world".getBytes(StandardCharsets.UTF_8));
        MessageHeader header = MessageHeader.builder(MessageHeader.Type.REQ_PUSH, topicName)
            .partition(partition)
            .messageLength(message.length())
            .build();

        CompletableFuture<MessageFrame> future = new CompletableFuture<>();
        requests.add(future);
        handler.channelRead(null, MessageFrame.of(header, message));

        MessageFrame frame = future.get();
        MessageHeader recHeader = frame.header();
        Message recMessage = frame.message();

        assertNotNull(recHeader);
        assertNotNull(recMessage);
    }
}
