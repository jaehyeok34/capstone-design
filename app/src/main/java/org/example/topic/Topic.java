package org.example.topic;

import io.netty.buffer.ByteBuf;

public interface Topic {

    void push(ByteBuf buf);
    TopicRecord pull();
    long getLength();

    public enum Type { MEMORY, DISK }
}
