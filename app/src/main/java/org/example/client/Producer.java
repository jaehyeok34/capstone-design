package org.example.client;

import java.nio.charset.StandardCharsets;

import org.example.message.Message;
import org.example.message.MessageHeader.Type;
import org.example.netty.client.NettyClient;

public class Producer implements AutoCloseable {

    private final NettyClient client;

    public Producer(int port) throws Exception {
        if (port <= 0 || port > 65535) throw new IllegalArgumentException("port: 유효하지 않은 값");

        this.client = new NettyClient(port);
    }

    public void request(String topicName, String payload) { 
        request(topicName, payload != null ? payload.getBytes(StandardCharsets.UTF_8) : null);
    }

    public void request(String topicName, byte[] payload) { 
        if (topicName == null || topicName.isEmpty()) throw new IllegalArgumentException("topicName: null or empty");
        if (payload == null || payload.length == 0) throw new IllegalArgumentException("payload: null or empty");

        Message msg = Message.of(topicName, payload);
        client.request(Type.REQ_PUSH, msg); 
    }

    @Override
    public void close() throws Exception { client.close(); }
}