package capstone.design.netty.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
    private final Map<String, CompletableFuture<Message>> requests = new ConcurrentHashMap<>();
    private final Channel channel;
    private final String clientId;
    private int requestCounter = 0;

    public NettyClient(String host, int port, String clientId) throws Exception {
        this.channel = createChannel(host, port);
        this.clientId = clientId;
    }

    private Channel createChannel(String host, int port) throws UnknownHostException, InterruptedException {
        Function<String, CompletableFuture<Message>> function = requests::get;

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

    public CompletableFuture<Message> fetch(Message message) {
        String requestId = String.valueOf(requestCounter++);
        message.addHeader(Map.of(
            "client.id", clientId,
            "request.id", requestId
        ));

        CompletableFuture<Message> future = new CompletableFuture<>();
        future.whenComplete((result, err) -> requests.remove(requestId));
        requests.put(requestId, future);

        channel.writeAndFlush(message);

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
            for (CompletableFuture<Message> request : requests.values()) {
                request.completeExceptionally(new IllegalStateException("채널 종료"));
            }

            requests.clear();
        }
    }
}
