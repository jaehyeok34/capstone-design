package capstone.design.topic;

import io.netty.util.ReferenceCounted;

public interface TopicRecord {
    int length();
    ReferenceCounted value();
    void release();
}
