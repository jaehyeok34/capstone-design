package capstone.design.client;

import java.util.concurrent.CompletableFuture;
import capstone.design.message.Message;
import capstone.design.message.MessageType;
import capstone.design.netty.client.NettyClient;

public class Producer implements AutoCloseable {

    private final NettyClient client;

    // constructor =====
    public Producer(String host, int port, String clientId) throws Exception {
        this.client = new NettyClient(host, port, clientId);
    }

    // method =====
    // 요청 후 응답 무시
    public void asyncProduce(Message message) {
        produce(message);
    }

    // 요청 후 응답까지 대기
    public Message syncProduce(Message message) {
        return produce(message).join();
    }

    private CompletableFuture<Message> produce(Message message) {
        message.setType(MessageType.REQ_PUSH);
        return client.fetch(message).get(0);
    }   
    
    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}