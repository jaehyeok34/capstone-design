package capstone.design.topic.memory;

import capstone.design.topic.TopicRecord;

public class MemoryRecord implements TopicRecord {

    private final byte[] buf;
    private final long createdTime;
    
    public MemoryRecord(byte[] buf) {
        this.buf = buf;
        this.createdTime = System.currentTimeMillis();
    }

    @Override
    public int length() { return buf.length; }
    @Override
    public Object value() { return buf; }

    public long createdTime() { return createdTime; }
    
    public boolean isExpired(long retention) {
        return System.currentTimeMillis() - createdTime >= retention;
    }
}
