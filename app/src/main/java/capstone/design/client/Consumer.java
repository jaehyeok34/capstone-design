package capstone.design.client;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

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
    public Message consume(Message message) {
        return client.request(message).join();
    }

    public Message consume(String topicName, int partition, long offset) {
        Message message = Message.of(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte(),
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        if (offset >= 0) {
            message.addOption(MessageOption.OFFSET, offset);
        }

        return consume(message);
    }

    public Message consume(String topicName, int partition) {
        return consume(topicName, partition, 0);
    }

    public ExecutorService subscribe(String topicName, int partition, Collection<Message> out) {
        return client.subscribe(topicName, partition, out);
    }

    /**
     * 특정 토픽/파티션을 구독하고, 새로운 메시지가 도착할 때마다 브로커에 요청하고, 그 결과를 out 큐에 추가
     * @param isAllConsume 구독한 토픽/파티션에 메시지가 존재하는 한 계속해서 consume 할지 여부
     */
    public ExecutorService subscribeAndConsume(boolean isAllConsume, String topicName, int partition, Collection<Message> out) {
        BlockingQueue<Message> notifiedQueue = new LinkedBlockingQueue<>();
        ExecutorService notifier = client.subscribe(topicName, partition, notifiedQueue);
        if (notifier == null) {
            return null; // 구독 실패
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    Message notified = notifiedQueue.take(); // blocking... 해제 시 push 됐다는 뜻
                    
                    /*
                     * TOPIC_UPDATED 알림 메시지에서 옵션을 활용하기 위해
                     * 메시지 타입만 변경하여 재활용
                     */
                    notified.addOption(MessageOption.MESSAGE_TYPE, MessageType.REQ_PULL.getByte());
                    
                    Long offset = notified.optionAsLong(MessageOption.OFFSET);
                    Long length = notified.optionAsLong(MessageOption.COUNT);

                    if (isAllConsume) {
                        /*
                         * isAllConsume(남은 메시지 모두 소비 여부)가 true이고,
                         * 남은 메시지 개수가 1개 이상이며,
                         * 다음 읽을 위치가 남은 메시지 개수 이내라면 계속 소비
                         * memory topic은 항상 "다음 읽을 위치 == 남은 메시지 개수"이므로 둘 다 0이여야 멈춤,
                         * disk topic은 "다음 읽을 위치 >= 남은 메시지 개수"여야 모두 소비한 상태이므로 멈춤
                         */
                        while (
                            length != null && length > 0 &&
                            offset != null && offset < length
                        ) {
                            /*
                             * consume()을 통해 획득한 메시지에서 정보를 추출하여
                             * 더 읽을 메시지가 있다면, 계속 읽기 위해 cursor, length 갱신
                             */
                            Message consumed = consume(notified);
                            out.add(consumed);
                            
                            offset = consumed.optionAsLong(MessageOption.OFFSET);
                            length = consumed.optionAsLong(MessageOption.COUNT);
                        }
                    } else { out.add(consume(notified)); }
                } catch (Exception e) {
                    System.err.println("NettyClient.subscribeAndConsume.executor 종료: " + e);
                    break;
                }
            }
            
            /*
             * 구독 알림 종료 shutdown() 하면 내부 InterruptedException 발생 안해서 종료 안됨
             * 또한, 해당 스레드가 종료되면 내부적으로 unsubscribe()를 호출하기 때문에 별도로 구독 해제 할 필요 없음
             */
            notifier.shutdownNow(); 
        });

        return executor;
    }


    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}
