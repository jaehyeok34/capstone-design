package org.example.client;

import java.nio.charset.StandardCharsets;

import org.example.message.Message;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.netty.client.NettyClient;

public class Producer implements AutoCloseable {

    private final NettyClient client;

    public Producer(int port) throws Exception {
        this.client = new NettyClient(port);
    }

    public void request(String topicName, int partition, byte[] payload) {
        if (topicName == null || topicName.isEmpty()) {
            throw new IllegalArgumentException("topicName: null or empty");
        }

        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("payload: null or empty");
        }

        Message message = Message.of(payload);
        MessageHeader header = MessageHeader.builder(MessageHeader.Type.REQ_PUSH, topicName)
            .partition(partition)
            .messageLength(message.length())
            .build();

        client.request(MessageFrame.of(header, message));
    }

    public void request(String topicName, int partition, String payload) {
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("payload: null or empty");
        }

        request(topicName, partition, payload.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}