package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import org.example.message.Message;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.netty.server.ServerInboundHandler;
import org.example.spy.SpyContext;
import org.example.topic.Topic;
import org.example.topic.TopicManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;

public class ServerInboundHandlerTest {
    
    ServerInboundHandler handler;
    TopicManager manager;
    final String topicName = "test_topic";
    final int partition = 1234;
    
    @BeforeEach
    void beforeEach() {
        manager = new TopicManager(Map.of(topicName, Topic.Type.MEMORY));
        handler = new ServerInboundHandler(manager);
    }

    @Test
    void noDataTest() throws Exception {
        var header = MessageHeader.builder(MessageHeader.Type.REQ_PULL, topicName)
            .partition(partition)
            .build();

        var context = new SpyContext();
        Queue<ByteBuf> queue = context.channel.queue;
        handler.channelRead(context, MessageFrame.of(header));
        assertEquals(1, queue.size());
        assertEquals(MessageHeader.Type.RES_PULL.getByte(), queue.poll().readByte());
    }

    @Test
    void channelReadTest() throws Exception {
        var message = Message.of("hello world".getBytes(StandardCharsets.UTF_8));
        var header1 = MessageHeader.builder(
            MessageHeader.Type.REQ_PUSH, topicName
        )   .partition(partition)
            .messageLength(message.length())
            .build();

        var context = new SpyContext();
        Queue<ByteBuf> queue = context.channel.queue;

        handler.channelRead(context, MessageFrame.of(header1, message));
        assertEquals(1, manager.topic(topicName).get().length(partition));
        assertEquals(1, queue.size()); // header가 잘 전달 됐는지 확인
        assertEquals(MessageHeader.Type.RES_PUSH.getByte(), queue.poll().readByte()); // RES_PUSH가 맞는지 확인

        var header2 = MessageHeader.builder(
            MessageHeader.Type.REQ_PULL, topicName
        )   .partition(partition)
            .build();

        handler.channelRead(context, MessageFrame.of(header2));
        assertEquals(0, manager.topic(topicName).get().length(partition));

        assertEquals(2, queue.size()); // header + message
        assertEquals(MessageHeader.Type.RES_PULL.getByte(), ((ByteBuf) queue.poll()).readByte());
    }
}
