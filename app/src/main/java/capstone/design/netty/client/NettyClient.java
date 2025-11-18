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

    public CompletableFuture<List<Message>> fetch(Message message) {
        String requestId = String.valueOf(requestCounter++);
        message.addHeader(Map.of(
            "client.id", clientId,
            "request.id", requestId
        ));

        /*
         * consumer.consume() 등에서 여러 개의 메시지를 요청할 수 있음
         * 이러한 경우를 대비해 요청 개수만큼 future를 생성하고
         * 합성 future를 반환하여 n개의 결과를 받을 수 있도록 CompletableFuture<List<Message>> 형태로 반환
         * 단, count가 없거나 1인 경우에도 List<Message> 형태이며, 내부 요소는 1개임
         */
        int count = Integer.parseInt(message.header("count", "1"));
        List<CompletableFuture<Message>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompletableFuture<Message> future = new CompletableFuture<>();
            futures.add(future);
        }
        requests.put(requestId, futures); // 요청 id에 futures 매핑

        /*
         * 모든 future가 완료(에러X) 되었을 경우, 각 future의 결과를 모아서 반환하는 combined future 생성
         * 요청이 모두 처리(에러 포함)된 후에는 requests 맵에서 해당 요청 id 제거
         * 내부 future 중 하나라도 에러가 발생하면, combined.join()에서 예외 발생
         */
        CompletableFuture<List<Message>> combined = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(ignored -> {
                List<Message> messages = new ArrayList<>();
                for (CompletableFuture<Message> future : futures) {
                    messages.add(future.join());
                }

                return messages;
            }).whenComplete((result, err) -> {
                requests.remove(requestId);
            });

        channel.writeAndFlush(message);

        return combined;
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
