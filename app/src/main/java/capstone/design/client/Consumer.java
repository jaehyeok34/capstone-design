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
            MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte(),
            MessageOption.CLIENT_ID, clientId,
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        if (cursor != null && cursor >= 0) {
            message.addOption(MessageOption.CURSOR, cursor);
        }

        return client.request(message).join();
    }

    public Message consume(String topicName, int partition) {
        return consume(topicName, partition, null);
    }

    /**
     * 특정 토픽/파티션을 구독하고, 새로운 메시지가 도착할 때마다 브로커에 요청하고, 그 결과를 out 큐에 추가
     * @param isAllConsume 구독한 토픽/파티션에 메시지가 존재하는 한 계속해서 consume 할지 여부
     */
    public ExecutorService subscribeAndConsume(boolean isAllConsume, String topicName, int partition, Queue<Message> out) {
        BlockingQueue<Message> notifiedQueue = new LinkedBlockingQueue<>();
        ExecutorService notifier = client.subscribe(topicName, partition, clientId, notifiedQueue);
        
        Utils.validate(notifier); // 구독 실패 시: notifier == null -> IllegalStateException 발생

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    Message notified = notifiedQueue.take(); // blocking... 해제 시 push 됐다는 뜻
                    
                    Long cursor = notified.option(MessageOption.CURSOR, Long.class);
                    Long remainingCount = notified.option(MessageOption.REMAINING_COUNT, Long.class);

                    if (isAllConsume) {
                        /*
                         * isAllConsume(남은 메시지 모두 소비 여부)가 true이고,
                         * 남은 메시지 개수가 1개 이상이며,
                         * 다음 읽을 위치가 남은 메시지 개수 이내라면 계속 consume
                         * memory topic은 항상 "다음 읽을 위치 == 남은 메시지 개수"이므로 둘 다 0이여야 멈춤,
                         * disk topic은 "다음 읽을 위치 >= 남은 메시지 개수"여야 모두 소비한 상태이므로 멈춤
                         */
                        while (
                            remainingCount != null && remainingCount > 0 &&
                            cursor != null && cursor < remainingCount
                        ) {
                            Message consumed = consume(topicName, partition); // 항상 최신 메시지 읽기
                            cursor = consumed.option(MessageOption.CURSOR, Long.class);
                            remainingCount = consumed.option(MessageOption.REMAINING_COUNT, Long.class);
                            
                            out.add(consumed);
                        }
                    } else {
                        out.add(consume(topicName, partition, cursor));
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("[debug] consume 중 예외 발생");
                }
                
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
