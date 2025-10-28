package capstone.design.topic.memory;

import capstone.design.Utils;
import capstone.design.topic.TopicRecord;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;

public class MemoryRecord implements TopicRecord {

    private final ByteBuf buf;
    
    private MemoryRecord(ByteBuf buf) {
        Utils.validate(buf);
        
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
