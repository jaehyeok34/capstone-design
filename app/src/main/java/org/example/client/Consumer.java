package org.example.client;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.example.Utils;
import org.example.message.Message;
import org.example.message.MessageOption;
import org.example.netty.client.NettyClient;
import org.jspecify.annotations.Nullable;


public class Consumer implements AutoCloseable {
    
    private final NettyClient client;
    private final String id;

    public Consumer(String host, int port, String id) throws Exception { 
        this.client = new NettyClient(host, port);
        this.id = (id != null && !id.isEmpty()) ? id : NettyClient.DEFAULT_ID;
    }
    public Consumer(String host, int port) throws Exception { this(host, port, null); }
    
    @Nullable
    public Message consume(String topicName, int partition, Long cursor) {
        Message message = new Message().addOptions(Map.of(
            MessageOption.TYPE, Message.Type.REQ_PULL.getByte(),
            MessageOption.ID, id,
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
        ExecutorService notifier = client.subscribe(topicName, partition, id, notifiedQueue);
        
        Utils.validate(notifier); // 구독 실패 시: notifier == null -> IllegalStateException 발생

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    Message notified = notifiedQueue.take(); // blocking...
                    Byte type = notified.option(MessageOption.TYPE, Byte.class);
                    if (type == null || type != Message.Type.TOPIC_UPDATE.getByte()) {
                        continue; // 유효한 알림이 아니면 무시
                    }
                    
                    // null일 수가 없음.. cursor를 생략하고 toByteBuf()를 호출하면 decoder 등에서 -1로 처리됨
                    Long cursor = notified.option(MessageOption.CURSOR, Long.class);
                    out.add(consume(topicName, partition, cursor));
                } catch (Exception ignored) {}
                
                System.out.println("[debug] blocking consume 종료");
                break;
            }
        });

        return executor;
    }

    public String id() { return id; }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
