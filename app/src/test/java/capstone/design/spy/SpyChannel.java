package capstone.design.spy;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class SpyChannel implements Channel {

    public Queue<Object> queue = new ArrayDeque<>();

    @Override
    public ChannelFuture write(Object msg) {
        queue.add(msg);
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        queue.add(msg);
        return null;
    }

    @Override
    public ChannelFuture closeFuture() {
        return null;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public EventLoop eventLoop() {
        return null;
    }

    @Override
    public ChannelId id() {
        return null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public ChannelMetadata metadata() {
        return null;
    }

    @Override
    public Channel parent() {
        return null;
    }

    @Override
    public ChannelPipeline pipeline() {
        return new SpyPipeline();
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return false;
    }

    @Override
    public int compareTo(Channel o) {
        return 0;
    }
}