package capstone.design.client;

import java.util.List;

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
        try {
            return client.fetch(message).join();
        } catch (Exception e) {
            System.err.println("Consumer.consume(): " + e);
            return List.of();
        }
    }

    public int find(Message message) {
        message.setType(MessageType.REQ_FIND);
        try {
            Message response = client.fetch(message).join().get(0);
            return Integer.parseInt(response.header("offset", "-1"));
        } catch (Exception e) {
            System.err.println("Consumer.find(): " + e);
            return -1;
        }
    }

    public boolean seek(Message message) {
        message.setType(MessageType.REQ_SEEK);
        try {
            Message response = client.fetch(message).join().get(0);
            return Boolean.parseBoolean(response.header("ok", "false"));
        } catch (Exception e) {
            System.err.println("Consumer.seek(): " + e);
            return false;
        }
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
