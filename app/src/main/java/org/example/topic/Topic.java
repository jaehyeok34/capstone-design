package org.example.topic;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface Topic {

    void push(int partition, String id, ByteBuf buf);
    @Nullable TopicRecord pull(int partition, String id, Long cursor);
    void subscribe(ChannelHandlerContext context, int partition, String id);
    void unsubscribe(int partition, String id);
    long length(int partition, String id);

    public enum Type { MEMORY, DISK }
}
