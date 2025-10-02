package org.example.client;

import java.net.UnknownHostException;

import org.example.message.Message;
import org.example.message.MessageHeader.Type;
import org.example.netty.NettyClient;

public class Consumer implements AutoCloseable {
    
    private final NettyClient client;

    public Consumer(int port) throws UnknownHostException, InterruptedException { 
        client = new NettyClient(port); 
    }

    public Message request(String topicName) { return client.request(Type.REQ_PULL, Message.of(topicName)).join(); }

    @Override
    public void close() throws Exception { client.close(); }
}
