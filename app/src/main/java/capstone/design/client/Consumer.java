package capstone.design.client;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.Nullable;

import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import capstone.design.netty.client.NettyClient;


public class Consumer implements AutoCloseable {
    
    private final NettyClient client;

    public Consumer(String host, int port, String clientId) throws Exception { 
        this.client = new NettyClient(host, port, clientId);
    }
    public Consumer(String host, int port) throws Exception { this(host, port, null); }

    public Message consume(Message message, int timeout, TimeUnit unit) throws Exception {
        message.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte());
        return client.request(message).get(timeout, unit);
    }

    @Nullable
    public ExecutorService subscribe(Message message, Collection<Message> out, int timeout, TimeUnit unit) throws Exception {
        return client.subscribe(message, out, timeout, unit);
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
