package capstone.design.client;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import capstone.design.netty.client.NettyClient;
import org.jspecify.annotations.Nullable;


public class Consumer implements AutoCloseable {
    
    private final NettyClient client;

    public Consumer(String host, int port, String clientId) throws Exception { 
        this.client = new NettyClient(host, port, clientId);
    }
    public Consumer(String host, int port) throws Exception { this(host, port, null); }

    @Nullable
    public Message consume(Message message, int timeout, TimeUnit unit) {
        try {
            return client.request(message).get(timeout, unit);
        } catch (Exception e) {
            System.err.println("Consumer.consume(): " + e);
            return null;
        }
    }

    public Message consume(String topicName, int partition, long offset, int timeout, TimeUnit unit) {
        Message message = Message.of(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte(),
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        if (offset >= 0) {
            message.addOption(MessageOption.OFFSET, offset);
        }

        return consume(message, timeout, unit);
    }

    public Message consume(String topicName, int partition, int timeout, TimeUnit unit) {
        return consume(topicName, partition, 0, timeout, unit);
    }

    public ExecutorService subscribe(Message message, Collection<Message> out, int timeout, TimeUnit unit) {
        return client.subscribe(message, out, timeout, unit);
    }

    public ExecutorService subscribe(String topicName, int partition, Collection<Message> out, int timeout, TimeUnit unit) {
        return client.subscribe(topicName, partition, out, timeout, unit);
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
