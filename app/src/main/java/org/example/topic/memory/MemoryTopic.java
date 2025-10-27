package org.example.topic.memory;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.example.topic.TopicRecord;
import org.example.topic.subscribe.SubscriberManager;
import org.example.Utils;
import org.example.topic.Topic;

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

        Queue<MemoryRecord> queue = partitionMap.computeIfAbsent(
            id, 
            ignored -> new ConcurrentLinkedQueue<>()  
        );

        queue.add(MemoryRecord.of(buf));
        buf.release();

        manager.notifyTo(partition, id);
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
        manager.subscribe(context, partition, id);
    }

    @Override
    public void unsubscribe(int partition, String id) {
        manager.unsubscribe(partition, id);
    }
}
