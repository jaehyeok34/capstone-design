package capstone.design.spy;

import java.net.SocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.EventExecutorGroup;

public class SpyPipeline implements ChannelPipeline {

    @Override
    public ChannelPipeline addAfter(String baseName, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addAfter(EventExecutorGroup group, String baseName, String name,
            ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addBefore(String baseName, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addBefore(EventExecutorGroup group, String baseName, String name,
            ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group, ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline addFirst(EventExecutorGroup group, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group, ChannelHandler... handlers) {
        return null;
    }

    @Override
    public ChannelPipeline addLast(EventExecutorGroup group, String name, ChannelHandler handler) {
        return null;
    }

    @Override
    public Channel channel() {
        return null;
    }

    @Override
    public ChannelHandlerContext context(ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(String name) {
        return null;
    }

    @Override
    public ChannelHandlerContext context(Class<? extends ChannelHandler> handlerType) {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelActive() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelInactive() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelRead(Object msg) {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelReadComplete() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelRegistered() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelUnregistered() {
        return null;
    }

    @Override
    public ChannelPipeline fireChannelWritabilityChanged() {
        return null;
    }

    @Override
    public ChannelPipeline fireExceptionCaught(Throwable cause) {
        return null;
    }

    @Override
    public ChannelPipeline fireUserEventTriggered(Object event) {
        return null;
    }

    @Override
    public ChannelHandler first() {
        return null;
    }

    @Override
    public ChannelHandlerContext firstContext() {
        return null;
    }

    @Override
    public ChannelPipeline flush() {
        return null;
    }

    @Override
    public ChannelHandler get(String name) {
        return null;
    }

    @SuppressWarnings("null")
    @Override
    public <T extends ChannelHandler> T get(Class<T> handlerType) {
        return null;
    }

    @Override
    public ChannelHandler last() {
        return null;
    }

    @Override
    public ChannelHandlerContext lastContext() {
        return null;
    }

    @Override
    public List<String> names() {
        return null;
    }

    @Override
    public ChannelPipeline remove(ChannelHandler handler) {
        return null;
    }

    @Override
    public ChannelHandler remove(String name) {
        return null;
    }

    @Override
    public <T extends ChannelHandler> T remove(Class<T> handlerType) {
        return null;
    }

    @Override
    public ChannelHandler removeFirst() {
        return null;
    }

    @Override
    public ChannelHandler removeLast() {
        return null;
    }

    @Override
    public ChannelPipeline replace(ChannelHandler oldHandler, String newName, ChannelHandler newHandler) {
        return null;
    }

    @Override
    public ChannelHandler replace(String oldName, String newName, ChannelHandler newHandler) {
        return null;
    }

    @Override
    public <T extends ChannelHandler> T replace(Class<T> oldHandlerType, String newName,
            ChannelHandler newHandler) {
        return null;
    }

    @Override
    public Map<String, ChannelHandler> toMap() {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelOutboundInvoker read() {
        return null;
    }

    @Override
    public ChannelPromise voidPromise() {
        return null;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return null;
    }

    @Override
    public Iterator<Entry<String, ChannelHandler>> iterator() {
        return null;
    }
}
