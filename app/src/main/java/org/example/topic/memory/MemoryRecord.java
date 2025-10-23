package org.example.topic.memory;

import org.example.topic.TopicRecord;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

public class MemoryRecord implements TopicRecord {

    private final ByteBuf buf;
    
    private MemoryRecord(ByteBuf buf) {
        if (buf == null || buf.readableBytes() == 0 || buf.refCnt() <= 0) {
            throw new IllegalArgumentException("value: null or empty or released");
        }
        
        this.buf = buf;
    }

    public static MemoryRecord of(ByteBuf buf) { return new MemoryRecord(buf); }
    
    @Override
    public int length() { return buf.readableBytes(); }
    @Override
    public ReferenceCounted value() { return buf; }
    @Override
    public void release() { 
        if (buf.refCnt() > 0) {
            buf.release(buf.refCnt());
        }   
    }
}
