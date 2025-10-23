package org.example.client;

import java.util.Optional;

import org.example.message.Message;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.netty.client.NettyClient;


public class Consumer implements AutoCloseable {
    
    private final NettyClient client;

    public Consumer(int port) throws Exception { 
        this.client = new NettyClient(port);
    }

    public Optional<Message> request(String topicName, int partition) {
        MessageHeader header = MessageHeader.builder(MessageHeader.Type.REQ_PULL, topicName)
            .partition(partition)
            .build();

        MessageFrame frame = client.request(MessageFrame.of(header)).join();
        return Optional.ofNullable(frame.message());
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
