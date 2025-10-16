package org.example.client;

import org.example.message.Message;
import org.example.message.MessageHeader.Type;
import org.example.netty.client.NettyClient;

public class Consumer implements AutoCloseable {
    
    private final NettyClient client;

    public Consumer(int port) throws Exception { 
        if (port <= 0 || port > 65535) throw new IllegalArgumentException("port: 유효하지 않은 값");

        this.client = new NettyClient(port); 
    }

    public Message request(String topicName) { 
        if (topicName == null || topicName.isEmpty()) throw new IllegalArgumentException("topicName: null or empty");
        
        Message message = Message.of(topicName);
        return client.request(Type.REQ_PULL, message).join(); 
    }

    @Override
    public void close() throws Exception { client.close(); }
}
