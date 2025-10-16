package org.example.topic.memory;

import org.example.topic.TopicRecord;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

public class MemoryRecord implements TopicRecord {

    private final ByteBuf value;
    
    public MemoryRecord(ByteBuf value) {
        if (value == null || value.readableBytes() == 0) throw new IllegalArgumentException("value: null 또는 빈 버퍼");

        System.out.println("[debug] MemoryRecord() - value.refCnt(): " + value.refCnt());
        this.value = value;
    }
    
    @Override
    public int getLength() { 
        System.out.println("[debug] MemoryRecord.getLength() - refCnt: " + value.refCnt());
        return value.readableBytes(); 
    }
    @Override
    public ReferenceCounted getValue() { return value; }
    @Override
    public void release() { value.release(value.refCnt()); }
}
