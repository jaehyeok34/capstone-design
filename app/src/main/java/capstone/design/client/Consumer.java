package capstone.design.client;

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
    public Message consume(Message message) throws Exception {
        message.setType(MessageType.REQ_PULL);
        return client.fetch(message).join();
    }

    public int find(Message message) {
        message.setType(MessageType.REQ_FIND);
        Message response = client.fetch(message).join();
        return Integer.parseInt(response.header("offset", "-1"));
    }

    public boolean seek(Message message) {
        message.setType(MessageType.REQ_SEEK);
        Message response = client.fetch(message).join();
        return Boolean.parseBoolean(response.header("ok", "false"));
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
