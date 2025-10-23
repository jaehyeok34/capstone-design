package org.example.netty.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import org.example.message.Message;
import org.example.message.MessageDecoder;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.netty.NettyInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {

    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final Queue<CompletableFuture<MessageFrame>> requests = new ArrayDeque<>();
    private final Channel channel;

    public NettyClient(int port) throws Exception {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port: 유효하지 않은 값");
        }

        channel = createChannel(port);
    }

    private Channel createChannel(int port) throws UnknownHostException, InterruptedException {
        NettyInitializer initializer = NettyInitializer.builder()
            .addHandler(new MessageDecoder())
            .addHandler(new ClientInboundHandler(requests::poll))
            .build();

        Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(initializer);
        
        ChannelFuture future = bootstrap.connect(InetAddress.getByName("localhost"), port).sync();
        return future.channel();
    }

    public CompletableFuture<MessageFrame> request(MessageFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("frame: null");
        }

        System.out.println("[debug]: 메시지 전송 시작");

        // 메시지 전송
        MessageHeader header = frame.header();
        channel.write(header.toByteBuf());

        Message message = frame.message();
        Optional.ofNullable(message)
            .ifPresentOrElse(
                msg -> channel.writeAndFlush(msg.toByteBuf()), 
                channel::flush
            );

        // 결과 대기용 future 생성
        CompletableFuture<MessageFrame> future = new CompletableFuture<>();
        requests.add(future);

        return future;
    }

    public void shutdownGracefully() {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }

        group.shutdownGracefully();
    }

    public void shutdown() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close().sync();
            }

            group.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            shutdownGracefully();
            requests.forEach(request -> {
                request.completeExceptionally(new IllegalStateException("채널 종료"));
            });

            requests.clear();
        }
    }
}
