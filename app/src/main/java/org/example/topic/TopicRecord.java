package org.example.topic;

import io.netty.util.ReferenceCounted;

public interface TopicRecord {
    int getLength();
    ReferenceCounted getValue();
    void release();
}
