package capstone.design.topic.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import capstone.design.topic.TopicRecord;
import capstone.design.topic.subscribe.SubscriberManager;
import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.topic.Topic;

import io.netty.channel.ChannelHandlerContext;

public class MemoryTopic implements Topic {

    private static final long DEFAULT_RETENTION = 30 * (60 * 1000); // 30분

    private final SubscriberManager subscriberManager = new SubscriberManager();
    private final Map<Integer, Map<String, List<MemoryRecord>>> topic = new ConcurrentHashMap<>();
    private final long retention;
    private final String name;

    private MemoryTopic(String name, long retention) {
        this.name = name;
        this.retention = retention;
    }

    public static MemoryTopic of(String name) {
        return new MemoryTopic(name, DEFAULT_RETENTION);
    }

    public static MemoryTopic of(String name, long retention) {
        return new MemoryTopic(name, retention);
    }

    public SubscriberManager subscriberManager() { return subscriberManager; }

    @Override
    public boolean push(int partition, String clientId, byte[] buf) {
        Utils.validate(clientId, buf);

        Map<String, List<MemoryRecord>> partitionMap = partitionMap(partition, clientId);

        // partition의 모든 구독자에게 레코드 추가
        for (List<MemoryRecord> storage : partitionMap.values()) {
            storage.add(new MemoryRecord(buf));
        }
        
        return true;    
    }

    @Override
    public TopicRecord pull(int partition, String clientId) {
        return pull(partition, clientId, -1);
    }
    
    @Override
    public TopicRecord pull(int partition, String clientId, long offset) {
        Utils.validate(clientId);

        List<MemoryRecord> storage = storage(partition, clientId);
        if (storage == null || storage.isEmpty()) {
            return null;
        }

        /*
         * offset이 long 타입이긴 하나, 내부적으로는 int 범위 내에서만 동작함
         * offset이 음수이면, FIFO 형태로 동작하여 0
         * 저장된 메시지 범위를 벗어나는 offset이면 null
         */
        offset = (offset < 0) ? 0 : offset;
        try {
            MemoryRecord record = storage.remove((int) offset);
            if (record.isExpired(retention)) {
                System.err.println("MemoryTopic.pull(): 만료된 레코드");
                return null;
            }

            return record;
        } catch (IndexOutOfBoundsException ignored) { return null; }

    }

    @Override
    public boolean notify(int partition, Message message) {
        return subscriberManager.notify(partition, message);
    }

    @Override
    public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
        partitionMap(partition, clientId); // 구독 전에 storage가 생성되도록 함
        subscriberManager.subscribe(context, partition, clientId);
    }

    @Override
    public void unsubscribe(int partition, String clientId) {
        topic.get(partition).remove(clientId); // storage도 같이 제거
        subscriberManager.unsubscribe(partition, clientId);
    }

    @Override
    public long count(int partition, String clientId) {
        List<MemoryRecord> storage = storage(partition, clientId);
        if (storage == null) {
            return 0;
        }

        return storage.size();
    }

    @Override
    public long offset(int partition, String clientId) {
        /*
         * 메시지가 저장된 적이 없는 partition/clientId 조합이라면 -1
         * 그 외에는 0 반환(FIFO이므로 항상 다음에 읽을 offset은 0)
         */
        return (storage(partition, clientId) != null) ? 0 : -1;
    }

    @Override
    public void clean() {
        /*
         * 모든 partition, 모든 storage(id에 따른 저장소)를 순회하며
         * retention 시간을 초과한 레코드 삭제
         */
        for (Map.Entry<Integer, Map<String, List<MemoryRecord>>> partitionEntry : topic.entrySet()) {
            int partition = partitionEntry.getKey();
            Map<String, List<MemoryRecord>> partitionMap = partitionEntry.getValue();

            for (Map.Entry<String, List<MemoryRecord>> entry : partitionMap.entrySet()) {
                String clientId = entry.getKey();
                List<MemoryRecord> storage = entry.getValue();
                
                Iterator<MemoryRecord> it = storage.iterator();
                while (it.hasNext()) {
                    if (it.next().isExpired(retention)) {
                        it.remove();
                        System.out.println("MemoryTopic.clean(): " + name + "." + partition + "." + clientId + "=" + storage.size());
                    }
                }
            }
        }
    }

    @Override
    public String name() { return name; }
    
    private List<MemoryRecord> storage(int partition, String clientId) {
        Map<String, List<MemoryRecord>> partitionMap = topic.get(partition);
        if (partitionMap == null) {
            return null;
        }

        return partitionMap.get(clientId);
    }

    /**
     * partition에 해당하는 partition map을 반환
     * 만약 존재하지 않는다면 새롭게 생성(client id에 해당하는 storage도 함께 생성)
     */
    private Map<String, List<MemoryRecord>> partitionMap(int partition, String clientId) {
        Map<String, List<MemoryRecord>> partitionMap = topic.computeIfAbsent(
            partition, ignored -> new ConcurrentHashMap<>()
        );

        partitionMap.computeIfAbsent(
            clientId, ignored -> Collections.synchronizedList(new ArrayList<>())
        );

        return partitionMap;
    }
}
