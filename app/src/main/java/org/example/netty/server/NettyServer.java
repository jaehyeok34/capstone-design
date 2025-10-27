package org.example.netty.server;

import org.example.Utils;
import org.example.message.MessageDecoder;
import org.example.message.MessageEncoder;
import org.example.message.MessageProcessor;
import org.example.netty.NettyInitializer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {

    private final int port;

    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(5, NioIoHandler.newFactory());
    private final ServerBootstrap bootstrap;
    private Channel channel;
    
    public NettyServer(int port, MessageProcessor processor) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port: " + port);
        }

        Utils.validate(processor);

        this.port = port;
        this.bootstrap = new ServerBootstrap()
            .group(workerGroup, bossGroup)
            .channel(NioServerSocketChannel.class);

        NettyInitializer initializer = NettyInitializer.builder()
            .addHandler(new MessageDecoder())
            .addHandler(new ServerInboundHandler(processor))
            .addHandler(new MessageEncoder())
            .build();

        bootstrap.childHandler(initializer);
    }

    public void start() throws InterruptedException {
        try {
            ChannelFuture future = bootstrap.bind(port).sync();
            this.channel = future.channel();

            channel.closeFuture().sync(); // 채널이 닫힐 때까지 대기(외부에서 닫으라는 이벤트 모니터링)
        } finally { shutdown(); }
    }

    public void shutdownGracefully() { // non-blocking
        if (channel != null && channel.isOpen()) {
            channel.close(); // 채널 닫기 요청
        }

        bossGroup.shutdownGracefully(); // 보스 그룹 종료 요청
        workerGroup.shutdownGracefully(); // 워커 그룹 종료 요청
    }

    public void shutdown() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close().sync();
            }

            bossGroup.shutdownGracefully().sync();
            workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            shutdownGracefully();
        }
    }

    public boolean isActive() {
        return (channel != null && channel.isActive()) && (!bossGroup.isShuttingDown()) && (!workerGroup.isShuttingDown());
    }
}
