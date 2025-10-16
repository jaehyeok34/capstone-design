package org.example.netty.server;

import org.example.message.MessageDecoder;
import org.example.message.MessageProcessor;
import org.example.netty.NettyInitializer;

import io.netty.bootstrap.ServerBootstrap;
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
    
    public NettyServer(int port, int maxFrameLength, MessageProcessor processor) {
        if (processor == null) throw new IllegalArgumentException("processor: null");

        this.port = port;
        this.bootstrap = new ServerBootstrap()
            .group(workerGroup, bossGroup)
            .channel(NioServerSocketChannel.class);

        NettyInitializer initializer = NettyInitializer.builder()
            .addHandler(new MessageDecoder())
            .addHandler(new ServerInboundHandler(processor))
            .build();

        bootstrap.childHandler(initializer);
    }

    public void start() throws InterruptedException {
        try {
            ChannelFuture f = bootstrap.bind(port).sync();
            f.channel().closeFuture().sync();
        } finally { close(); }
    }

    public void close() {
        if (bossGroup != null)  bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }
}
