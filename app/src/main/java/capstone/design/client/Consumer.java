package capstone.design.client;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import capstone.design.netty.client.NettyClient;
import org.jspecify.annotations.Nullable;


public class Consumer implements AutoCloseable {
    
    private final NettyClient client;
    private final String clientId;

    public Consumer(String host, int port, String clientId, String filePath) throws Exception { 
        this.client = new NettyClient(host, port, filePath);
        this.clientId = (clientId != null && !clientId.isEmpty()) ? clientId : NettyClient.DEFAULT_ID;
    }
    public Consumer(String host, int port, String clientId) throws Exception { this(host, port, clientId, null); }
    public Consumer(String host, int port) throws Exception { this(host, port, null, null); }
    
    @Nullable
    public Message consume(String topicName, int partition, Long cursor) {
        Message message = new Message().addOptions(Map.of(
            MessageOption.TYPE, MessageType.REQ_PULL.getByte(),
            MessageOption.CLIENT_ID, clientId,
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        if (cursor != null) {
            message.addOption(MessageOption.CURSOR, cursor);
        }

        return client.request(message).join();
    }

    public Message consume(String topicName, int partition) {
        return consume(topicName, partition, null);
    }

    public ExecutorService subscribeAndConsume(String topicName, int partition, Queue<Message> out) {
        BlockingQueue<Message> notifiedQueue = new LinkedBlockingQueue<>();
        ExecutorService notifier = client.subscribe(topicName, partition, clientId, notifiedQueue);
        
        Utils.validate(notifier); // 구독 실패 시: notifier == null -> IllegalStateException 발생

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    Message notified = notifiedQueue.take(); // blocking...
                    Byte type = notified.option(MessageOption.TYPE, Byte.class);
                    if (type == null || type != MessageType.TOPIC_UPDATED.getByte()) {
                        continue; // 유효한 알림이 아니면 무시
                    }
                    
                    Long cursor = notified.option(MessageOption.CURSOR, Long.class);
                    out.add(consume(topicName, partition, cursor));
                } catch (Exception ignored) {}
                
                System.out.println("[debug] blocking consume 종료");
                break;
            }
        });

        return executor;
    }

    public String id() { return clientId; }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
