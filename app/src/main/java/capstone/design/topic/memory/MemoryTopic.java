package capstone.design.topic.memory;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import capstone.design.topic.TopicRecord;
import capstone.design.topic.subscribe.SubscriberManager;
import capstone.design.Utils;
import capstone.design.topic.Topic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class MemoryTopic implements Topic {

    private final SubscriberManager manager;
    private final Map<Integer, Map<String, Queue<MemoryRecord>>> storage = new ConcurrentHashMap<>();

    public MemoryTopic(String name) {
        Utils.validate(name);

        this.manager = new SubscriberManager(name);
    }

    public SubscriberManager subscriberManager() { return manager; }

    /**
     * @param buf 전달 시 반드시 retain() 된 상태(refCnt() > 1) 이어야 함
     * 내부적으로 buf의 참조 카운트를 1 감소시키기 때문.(최소 1은 유지해야 함)
     */
    @Override
    public void push(int partition, String clientId, ByteBuf buf) {
        Utils.validate(clientId, buf);

        Map<String, Queue<MemoryRecord>> partitionMap = storage.computeIfAbsent(
            partition, 
            ignored -> new ConcurrentHashMap<>()
        );

        // partition에 id에 해당하는 큐가 없다면 새롭게 생성
        partitionMap.computeIfAbsent(clientId, ignored -> new ConcurrentLinkedQueue<>());
    
        // partition의 모든 구독자에게 레코드 추가
        for (Queue<MemoryRecord> queue : partitionMap.values()) {
            queue.add(MemoryRecord.of(buf));
        }

        buf.release();

        // partition의 모든 구독자들에게 알림
        manager.notify(
            partition,
            cursor(partition, clientId),
            offset(partition, clientId),
            remainingCount(partition, clientId)
        );
    }
    
    /**
     * @param cursor disk topic과 동일한 메서드 시그니처를 유지하기 위해 존재(사용하지 않음)
     */
    @Override
    public TopicRecord pull(int partition, String clientId, long cursor) {
        Utils.validate(clientId);

        Map<String, Queue<MemoryRecord>> partitionMap = storage.get(partition);
        if (partitionMap == null) {
            return null;
        }

        Queue<MemoryRecord> queue = partitionMap.get(clientId);
        if (queue == null) {
            return null;
        }

        return queue.poll();
    }
    public TopicRecord pull(int partition, String id) { return pull(partition, id, -1); }

    @Override
    public long length(int partition, String clientId) {
        Map<String, Queue<MemoryRecord>> partitionMap = storage.get(partition);
        if (partitionMap == null) {
            return 0;
        }

        Queue<MemoryRecord> queue = partitionMap.get(clientId);
        if (queue == null) {
            return 0;
        }

        return queue.size();
    }

    @Override
    public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
        // 구독하려는 partition/id에 해당하는 큐가 없다면 새롭게 생성
        storage.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(clientId, ignored -> new ConcurrentLinkedQueue<>());

        manager.subscribe(context, partition, clientId);
    }

    @Override
    public void unsubscribe(int partition, String clientId) {
        // partition/id에 해당하는 큐 삭제
        Map<String, Queue<MemoryRecord>> partitionMap = storage.get(partition);
        if (partitionMap != null) {
            partitionMap.remove(clientId);
        }

        manager.unsubscribe(partition, clientId);
    }

    /**
     * memory topic에서는 cursor(읽기 위치)가 length와 동일함
     */
    @Override
    public long cursor(int partition, String clientId) {
        return length(partition, clientId);
    }

    @Override
    public long offset(int partition, String clientId) {
        return length(partition, clientId);
    }

    @Override
    public long remainingCount(int partition, String clientId) {
        return length(partition, clientId);
    }
}
