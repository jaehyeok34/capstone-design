package org.example.netty;

import java.util.ArrayList;
import java.util.List;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;

public class NettyInitializer extends ChannelInitializer<Channel> {

    private final List<ChannelHandler> handlers;

    private NettyInitializer(Builder builder) {
        if (builder.handlers == null || builder.handlers.isEmpty()) {
            throw new IllegalArgumentException("handlers: null 또는 비어있음");
        }

        this.handlers = builder.handlers;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        handlers.forEach(ch.pipeline()::addLast);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final List<ChannelHandler> handlers = new ArrayList<>();

        private Builder() {}

        public Builder addHandler(ChannelHandler handler) {
            handlers.add(handler);
            return this;
        }

        public NettyInitializer build() { return new NettyInitializer(this); }
    }
}
