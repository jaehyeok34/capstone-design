package capstone.design.netty.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import capstone.design.message.Message;
import capstone.design.message.MessageDecoder;
import capstone.design.message.MessageEncoder;
import capstone.design.netty.NettyInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {

    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final Map<String, List<CompletableFuture<Message>>> requests = new ConcurrentHashMap<>();
    private final Channel channel;
    private final String clientId;
    private int requestCounter = 0;

    public NettyClient(String host, int port, String clientId) throws Exception {
        this.channel = createChannel(host, port);
        this.clientId = clientId;
    }

    private Channel createChannel(String host, int port) throws UnknownHostException, InterruptedException {
        Function<String, List<CompletableFuture<Message>>> function = requests::get;

        NettyInitializer initializer = NettyInitializer.builder()
            .addHandler(MessageDecoder.class)
            .addHandler(ClientInboundHandler.class, function)
            .addHandler(MessageEncoder.class)
            .build();

        Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(initializer);

        ChannelFuture future = bootstrap.connect(InetAddress.getByName(host), port).sync();
        return future.channel();
    }

    // public CompletableFuture<List<Message>> fetch(Message message) {
    public List<CompletableFuture<Message>> fetch(Message message) {
        String requestId = String.valueOf(requestCounter++);
        message.addHeader(Map.of(
            "client.id", clientId,
            "request.id", requestId
        ));

        /*
         * consumer.consume() 등에서 여러 개의 메시지를 요청할 수 있음
         * 이러한 경우를 대비해 요청 개수만큼 future를 생성하여 처리
         * 단, count가 없거나 1인 경우에도 List<Message> 형태이며, 내부 요소는 1개임
         */
        int count = Integer.parseInt(message.header("count", "1"));
        List<CompletableFuture<Message>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            futures.add(future);
        }
        requests.put(requestId, futures); // 요청 id에 futures 매핑

        // 요청이 모두 처리되면(에러 포함) requests에서 해당 id 항목 제거
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .whenComplete((result, err) -> {
                requests.remove(requestId);
            });


        channel.writeAndFlush(message);

        return futures;
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
            for (List<CompletableFuture<Message>> request : requests.values()) {
                for (CompletableFuture<Message> future : request) {
                    future.completeExceptionally(new IllegalStateException("채널 종료"));
                }
            }

            requests.clear();
        }
    }
}
