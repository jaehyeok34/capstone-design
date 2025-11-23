package capstone.design.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import capstone.design.message.Message;
import capstone.design.message.MessageType;
import capstone.design.netty.client.NettyClient;


public class Consumer implements AutoCloseable {
    
    private final NettyClient client;

    // constructor =====
    public Consumer(String host, int port, String clientId) throws Exception { 
        this.client = new NettyClient(host, port, clientId);
    }

    // method =====
    public List<Message> consume(Message message) {
        message.setType(MessageType.REQ_PULL);
        List<Message> responses = new ArrayList<>();
        for (CompletableFuture<Message> future : client.fetch(message)) {
            try {
                responses.add(future.join());
            } catch (Exception e) {
                System.err.println("? Consumer.consume(): future 예외 발생: " + e);
            }
        }

        return responses;
    }

    public int find(Message message) {
        message.setType(MessageType.REQ_FIND);
        try {
            Message response = client.fetch(message).get(0).join();
            return Integer.parseInt(response.header("offset", "-1"));
        } catch (Exception e) {
            System.err.println("? Consumer.find(): " + e);
            return -1;
        }
    }

    public boolean seek(Message message) {
        message.setType(MessageType.REQ_SEEK);
        try {
            Message response = client.fetch(message).get(0).join();
            return response.header("error", "").isEmpty();
        } catch (Exception e) {
            System.err.println("? Consumer.seek(): " + e);
            return false;
        }
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
