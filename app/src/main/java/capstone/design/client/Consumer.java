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
    public Message consume(Message message) {
        return client.request(message).join();
    }

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

        return consume(message);
    }

    public Message consume(String topicName, int partition) {
        return consume(topicName, partition, null);
    }

    public ExecutorService subscribe(String topicName, int partition, Queue<Message> out) {
        return client.subscribe(topicName, partition, clientId, out);
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

                    /*
                     * TOPIC_UPDATED 알림 메시지에 데이터를 활용하기 위해
                     * 메시지 타입만 변경하여 재활용
                     */
                    notified.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte());
                    
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
                            /*
                             * consume()을 통해 획득한 메시지에서 정보를 추출하여
                             * 더 읽을 메시지가 있다면, 계속 읽기 위해 cursor, remainingCount 갱신
                             */
                            Message consumed = consume(notified);
                            cursor = consumed.option(MessageOption.CURSOR, Long.class);
                            remainingCount = consumed.option(MessageOption.REMAINING_COUNT, Long.class);
                            
                            out.add(consumed);
                        }
                    } else { out.add(consume(notified)); }
                } catch (Exception e) {
                    System.err.println("NettyClient.subscribeAndConsume.executor 종료: " + e);
                    break;
                }
            }

            notifier.shutdownNow(); // 구독 알림 종료 shutdown()했을 시 내부 InterruptedException 발생 안해서 종료 안됨
        });

        return executor;
    }

    public String clientId() { return clientId; }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
