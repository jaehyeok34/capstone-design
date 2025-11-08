package capstone.design.client;

import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import capstone.design.netty.client.NettyClient;

public class Producer implements AutoCloseable {

    private final NettyClient client;

    public Producer(String host, int port, String clientId) throws Exception {
        this.client = new NettyClient(host, port, clientId);
    }
    public Producer(String host, int port) throws Exception { this(host, port, null); }

    /**
     * 서버에 REQ_PUSH 메시지 보내고 바로 반환
     */
    public void asyncProduce(Message message) {
        message.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PUSH.getByte());
        client.command(message);
    }

    /**
     * 서버에 REQ_PUSH 메시지 보내고, 서버에서 응답이 올 때까지 대기
     */
    @Nullable
    public Message syncProduce(Message message, int timeout, TimeUnit unit) throws Exception {
        message.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PUSH.getByte());
        return client.request(message).get(timeout, unit);
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}