package capstone.design.netty.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageDecoder;
import capstone.design.message.MessageEncoder;
import capstone.design.message.MessageOption;
import capstone.design.netty.NettyInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {

    public static final String DEFAULT_ID = "anonymous";

    private final EventLoopGroup group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
    private final Queue<CompletableFuture<Message>> requests = new ArrayDeque<>();
    private final Map<String, Map<Integer, BlockingQueue<Message>>> subscriptions = new ConcurrentHashMap<>();
    private final Channel channel;

    public NettyClient(String host, int port) throws Exception {
        Utils.validate(host);
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port: 유효하지 않은 값");
        }

        channel = createChannel(host, port);
    }

    private Channel createChannel(String host, int port) throws UnknownHostException, InterruptedException {
        Supplier<CompletableFuture<Message>> supplier = requests::poll;
        BiFunction<String, Integer, BlockingQueue<Message>> queueProvider = (topicName, partition) -> {
            Map<Integer, BlockingQueue<Message>> partitionMap = subscriptions.get(topicName);
            if (partitionMap == null) {
                return null;
            }

            return partitionMap.get(partition);
        };

        NettyInitializer initializer = NettyInitializer.builder()
            .addHandler(MessageDecoder.class)
            .addHandler(ClientInboundHandler.class, new Class<?>[] { Supplier.class, BiFunction.class }, supplier, queueProvider)
            .addHandler(MessageEncoder.class)
            .build();

        Bootstrap bootstrap = new Bootstrap()
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(initializer);

        ChannelFuture future = bootstrap.connect(InetAddress.getByName(host), port).sync();
        return future.channel();
    }

    /**
     * 서버에 메시지를 전송하고, 응답을 future에 전달함(request <-> response)
     * @return 서버의 응답을 담는 future
     */
    public CompletableFuture<Message> request(Message message) {
        Utils.validate(message);

        // // 결과 대기용 future 생성
        CompletableFuture<Message> future = new CompletableFuture<>();
        requests.add(future);

        // 메시지 전송
        channel.writeAndFlush(message);

        return future;
    }

    /**
     * 서버에 메시지 전송만 함(응답 무시)
     */
    public void command(Message message) {
        Utils.validate(message);
        channel.writeAndFlush(message);
    }

    /**
     * 특정 토픽/파티션을 구독하고, 메시지가 업데이트되면 out 큐에 TOPIC_UPDATE를 포함한 정보가 담김
     * 따라서, out 큐를 blocking하게 모니터링해서 TOPIC_UPDATE 메시지를 처리할 수 있음
     */
    public ExecutorService subscribe(String topicName, int partition, String id, Queue<Message> out) {
        Utils.validate(topicName, id, out);

        Message subscribeMsg = new Message().addOptions(Map.of(
            MessageOption.TYPE, Message.Type.REQ_SUBSCRIBE.getByte(),
            MessageOption.ID, id,
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        // 구독 요청
        Message subscribeResponse = request(subscribeMsg).join(); // 구독 요청 대기
        byte responseType = subscribeResponse.option(MessageOption.TYPE, Byte.class);
        
        // 구독 성공 시
        if (responseType == Message.Type.RES_SUBSCRIBE.getByte()) {
            Map<Integer, BlockingQueue<Message>> partitionMap = subscriptions.computeIfAbsent(
                topicName, 
                ignored -> new ConcurrentHashMap<>()
            );

            BlockingQueue<Message> queue = partitionMap.computeIfAbsent(
                partition, 
                ignored -> new LinkedBlockingDeque<>()
            );

            partitionMap.put(partition, queue); // partition 맵 갱신
            subscriptions.put(topicName, partitionMap); // 토픽 구독 맵 갱신

            // 별도의 스레드에서 알람 수신 대기
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                while (true) {
                    try {
                        Message updateMsg = queue.take(); // blocking
                        out.add(updateMsg);
                    } catch (Exception ignored) {}
    
                    break;
                }

                partitionMap.remove(partition); // 모니터링 종료 후 파티션 맵에서 제거
                unsubscribe(topicName, partition, id); // 구독 해제
            });

            return executor;
        }

        return null;
    }

    public void unsubscribe(String topicName, int partition, String id) {
        Utils.validate(topicName, id);

        Message unsubscribeMsg = new Message().addOptions(Map.of(
            MessageOption.TYPE, Message.Type.REQ_UNSUBSCRIBE.getByte(),
            MessageOption.ID, id,
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        // 구독 취소 요청
        request(unsubscribeMsg);
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
