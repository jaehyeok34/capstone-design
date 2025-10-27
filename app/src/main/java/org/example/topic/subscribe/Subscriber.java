package org.example.topic.subscribe;

import io.netty.channel.ChannelHandlerContext;

public record Subscriber(ChannelHandlerContext context, String id) {

    public Subscriber {
        if (context == null) {
            throw new IllegalArgumentException("context: null");
        }

        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id: null or empty");
        }
    }
}
