package org.example.netty;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.example.message.Message;
import org.example.message.MessageDecoder;
import org.example.message.MessageHeader;
import org.example.message.MessageDecoder.MessageFrame;
import org.example.message.MessageHeader.Type;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

// 실제 netty server에 메시지를 전송하는 클라이언트
public class NettyClient {
    
    private int request_id_counter = 0; // Integer.MIN_VALUE ~ Integer.MAX_VALUE 순환
    private final Map<Integer, CompletableFuture<Message>> requests = new ConcurrentHashMap<>();
    private final EventLoopGroup group;
    private final Channel channel;

    public NettyClient(int port) throws UnknownHostException, InterruptedException {
        group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        channel = createChannel(port);
    }

    private Channel createChannel(int port) throws UnknownHostException, InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    // inbound
                    pipeline.addLast(new MessageDecoder());
                    pipeline.addLast(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            System.out.println("[debug]: 서버 응답 수신");

                            MessageFrame frame = (MessageFrame) msg;
                            CompletableFuture<Message> future = requests.get(frame.header().getRequestId());

                            if (future != null) future.complete(frame.message());

                            System.out.println("[debug]: CompletableFuture에 전달 완료");

                            super.channelRead(ctx, msg);
                        }
                    });

                    // outbound
                }
            });
        
        ChannelFuture future = bootstrap.connect(InetAddress.getByName("localhost"), port).sync();
        return future.channel();
    }

    public CompletableFuture<Message> request(Type type, Message message) {
        if (channel == null || !channel.isActive()) throw new IllegalStateException("채널이 연결되어 있지 않음");

        System.out.println("[debug]: 메시지 전송 시작");
        int id = request_id_counter++;
        MessageHeader header = MessageHeader.of(type, id, message.getLength());

        channel.writeAndFlush(MessageFrame.wrapToByteBuf(header, message));
        System.out.println("[debug]: 메시지 전송 완료");

        if (type == Type.REQ_PUSH) return null; // producer 요청 처리(결과 필요 X)

        CompletableFuture<Message> future = new CompletableFuture<>();
        future.whenComplete((result, ex) -> requests.remove(id)); // 완료되면 맵에서 제거
        requests.put(id, future); // consumer 요청 처리(결과 필요 O, 요청 ID 매핑 테이블 작성)

        System.out.println("[debug]: requests 개수: " + requests.size());
        return future;
    }

    public void close() throws Exception {
        if (channel != null && channel.isOpen()) channel.close().sync();
        if (group != null && !group.isShuttingDown()) group.shutdownGracefully().sync();
        requests.forEach((id, future) -> future.completeExceptionally(new IllegalStateException("채널이 종료됨")));
        requests.clear();
    }
}
