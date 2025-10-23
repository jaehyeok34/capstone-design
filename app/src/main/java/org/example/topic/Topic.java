package org.example.topic;

import java.util.Optional;

import io.netty.buffer.ByteBuf;

public interface Topic {

    void push(int partition, ByteBuf buf);
    Optional<TopicRecord> pull(int partition);
    long length(int partition);

    public enum Type { MEMORY, DISK }
}
