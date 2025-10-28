package capstone.design.topic.memory;

import java.util.Map;
import java.util.Optional;
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

    public SubscriberManager subscriberManager() {
        return manager;
    }

    /*
     * ByteBuf buf 전달 시 반드시 retain() 된 상태(refCnt() > 1) 이어야 함
     * 내부적으로 buf의 참조 카운트를 1 감소시키기 때문.(최소 1은 유지해야 함)
     */
    @Override
    public void push(int partition, String id, ByteBuf buf) {
        Utils.validate(id, buf);

        Map<String, Queue<MemoryRecord>> partitionMap = storage.computeIfAbsent(
            partition, 
            ignored -> new ConcurrentHashMap<>()
        );

        // partition에 id에 해당하는 큐가 없다면 새롭게 생성
        partitionMap.computeIfAbsent(id, ignored -> new ConcurrentLinkedQueue<>());
    
        // partition의 모든 구독자에게 레코드 추가
        for (var queue : partitionMap.values()) {
            queue.add(MemoryRecord.of(buf));
        }

        buf.release();

        // partition의 모든 구독자드에게 알림
        manager.notify(partition);
    }
    
    @Override
    public TopicRecord pull(int partition, String id, Long cursor) {
        return Optional.ofNullable(storage.get(partition))
            .map(partitionMap -> partitionMap.get(id))
            .map(Queue::poll)
            .orElse(null);
    }
    public TopicRecord pull(int partition, String id) { return pull(partition, id, null); }

    @Override
    public long length(int partition, String id) {
        return Optional.ofNullable(storage.get(partition))
            .map(partitionMap -> partitionMap.get(id))
            .map(Queue::size)
            .orElse(0);
    }

    @Override
    public void subscribe(ChannelHandlerContext context, int partition, String id) {
        // 구독하려는 partition/id에 해당하는 큐가 없다면 새롭게 생성
        storage.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(id, ignored -> new ConcurrentLinkedQueue<>());

        manager.subscribe(context, partition, id);
    }

    @Override
    public void unsubscribe(int partition, String id) {
        // partition/id에 해당하는 큐 삭제
        var partitionMap = storage.get(partition);
        if (partitionMap != null) {
            partitionMap.remove(id);
        }

        manager.unsubscribe(partition, id);
    }
}
