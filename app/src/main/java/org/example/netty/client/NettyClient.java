package org.example.netty.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.example.message.Message;
import org.example.message.MessageDecoder;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.message.MessageHeader.Type;
import org.example.netty.NettyInitializer;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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

    public NettyClient(int port) throws Exception {
        if (port <= 0 || port > 65535) throw new IllegalArgumentException("port: 유효하지 않은 값");

        group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        channel = createChannel(port);
    }

    private Channel createChannel(int port) throws UnknownHostException, InterruptedException {
        NettyInitializer initializer = NettyInitializer.builder()
            .addHandler(new MessageDecoder())
            .addHandler(new ClientInboundHandler(requests::get))
            .build();

        Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(initializer);
        
        ChannelFuture future = bootstrap.connect(InetAddress.getByName("localhost"), port).sync();
        return future.channel();
    }

    public CompletableFuture<Message> request(Type type, Message message) {
        if (type == null) throw new IllegalArgumentException("type: null");
        if (message == null) throw new IllegalArgumentException("message: null");
        if (channel == null || !channel.isActive()) throw new IllegalStateException("채널이 연결되어 있지 않음");

        System.out.println("[debug]: 메시지 전송 시작");
        int id = request_id_counter++;
        MessageHeader header = MessageHeader.of(type, id, message.getLength());
        ByteBuf buf = MessageFrame.ofByteBuf(header, message).retain();

        channel.writeAndFlush(buf);
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
        if (channel.isOpen()) throw new IllegalStateException("채널 종료 실패");

        if (group != null && !group.isShuttingDown()) group.shutdownGracefully().sync();
        if (!group.awaitTermination(10, TimeUnit.SECONDS)) throw new IllegalStateException("EventLoopGroup 종료 실패");

        requests.forEach((id, future) -> future.completeExceptionally(new IllegalStateException("채널이 종료됨")));
        requests.clear();
    }
}
