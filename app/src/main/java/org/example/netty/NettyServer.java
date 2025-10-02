package org.example.netty;

import org.example.broker.topic.Topic.Record;
import org.example.message.Message;
import org.example.message.MessageDecoder;
import org.example.message.MessageRepository;
import org.example.message.MessageDecoder.MessageFrame;
import org.example.message.MessageHeader;
import org.example.message.MessageHeader.Type;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NettyServer {

    private final int port;

    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(5, NioIoHandler.newFactory());
    private final ServerBootstrap bootstrap;
    
    public NettyServer(int port, int maxFrameLength, MessageRepository repository) {
        this.port = port;
        this.bootstrap = new ServerBootstrap();

        initialize(maxFrameLength, repository);
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

    private void initialize(int maxFrameLength, MessageRepository repository) {
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();

                    // inbound
                    p.addLast(new MessageDecoder());
                    p.addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            System.out.println("[debug]: 메시지를 수신했습니다.");

                            MessageFrame frame = (MessageFrame) msg;
                            MessageHeader header = frame.header();
                            Message message = frame.message();

                            // processing
                            if (header.getType() == Type.REQ_PULL) {
                                Record record = repository.pull(message.getTopicName()); 
                                Channel channel = ctx.channel();
                                MessageHeader resHeader = MessageHeader.of(Type.RES_PULL, header.getRequestId(), record != null ? record.length() : -1);

                                channel.write(resHeader.toByteBuf());
                                if (record != null) channel.writeAndFlush(record.value());
                                else channel.flush();
                            } else {
                                repository.push(message.getTopicName(), message.getPayload()); 
                                // TODO: producer 응답 언젠가는 구현해보자...
                            }

                            System.out.println("[debug]: 메시지 처리 완료");
                            super.channelRead(ctx, msg);
                        }
                    });

                    // outbound
                }
            });
    }
}
