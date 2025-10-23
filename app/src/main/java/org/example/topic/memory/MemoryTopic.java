package org.example.topic.memory;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.example.topic.TopicRecord;
import org.example.topic.Topic;

import io.netty.buffer.ByteBuf;

public class MemoryTopic implements Topic {

    private final Map<Integer, Queue<MemoryRecord>> storage = new ConcurrentHashMap<>();

    private MemoryTopic() {}

    public static MemoryTopic of() { return new MemoryTopic(); }

    /*
     * ByteBuf buf 전달 시 반드시 retain() 된 상태(refCnt() > 1) 이어야 함
     * 내부적으로 buf의 참조 카운트를 1 감소시키기 때문.(최소 1은 유지해야 함)
     */
    @Override
    public void push(int partition, ByteBuf buf) {
        if (buf == null || buf.readableBytes() == 0) {
            throw new IllegalArgumentException("buf: null 또는 비어 있음");
        }

        if (buf.refCnt() <= 1) {
            throw new IllegalStateException("buf: 참조 카운트 부족(2 이상이어야 함)");
        }

        Queue<MemoryRecord> queue = storage.computeIfAbsent(
            partition, 
            __ -> new ConcurrentLinkedQueue<>()
        );

        queue.add(MemoryRecord.of(buf));
        buf.release();
    }
    
    @Override
    public Optional<TopicRecord> pull(int partition) {
        return Optional.ofNullable(storage.get(partition))
            .map(queue -> queue.poll());
    }

    @Override
    public long length(int partition) {
        return Optional.ofNullable(storage.get(partition))
            .map(Queue::size)
            .orElse(0);
    }
}
