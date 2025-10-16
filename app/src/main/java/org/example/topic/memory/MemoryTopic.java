package org.example.topic.memory;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.example.topic.TopicRecord;
import org.example.topic.Topic;

import io.netty.buffer.ByteBuf;

public class MemoryTopic implements Topic {

    private final ConcurrentLinkedQueue<MemoryRecord> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void push(ByteBuf buf) {
        if (buf == null || buf.readableBytes() == 0) throw new IllegalArgumentException("buf: null 또는 비어 있음");
        
        queue.add(new MemoryRecord(buf));
    }
    
    @Override
    public TopicRecord pull() {
        MemoryRecord record = queue.poll();
        System.out.println("[debug] MemoryTopic.pull() - refCnt: " + (record != null ? record.getValue().refCnt() : "null"));
        
        return record;
    }

    @Override
    public long getLength() { return queue.size(); }
}
