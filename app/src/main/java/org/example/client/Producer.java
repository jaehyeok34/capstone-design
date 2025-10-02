package org.example.client;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.example.message.Message;
import org.example.message.MessageHeader.Type;
import org.example.netty.NettyClient;

public class Producer implements AutoCloseable {

    private final NettyClient client;

    public Producer(int port) throws UnknownHostException, InterruptedException {
        client = new NettyClient(port);
    }

    public void request(String topicName, String payload) { 
        Message msg = Message.of(topicName, payload.getBytes(StandardCharsets.UTF_8));
        client.request(Type.REQ_PUSH, msg); 
    }

    public void request(String topicName, byte[] payload) { 
        Message msg = Message.of(topicName, payload);
        client.request(Type.REQ_PUSH, msg); 
    }

    @Override
    public void close() throws Exception { client.close(); }
}