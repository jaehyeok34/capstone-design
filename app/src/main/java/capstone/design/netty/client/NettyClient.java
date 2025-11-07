package capstone.design.netty.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Function;
import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageDecoder;
import capstone.design.message.MessageEncoder;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
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
    private final Map<Integer, CompletableFuture<Message>> requests = new ConcurrentHashMap<>();
    private final Map<String, Map<Integer, BlockingQueue<Message>>> subscriptions = new ConcurrentHashMap<>();
    private final Channel channel;
    private final String clientId;
    private int requestCounter = 0;

    public NettyClient(String host, int port, String clientId) throws Exception {
        Utils.validate(host);
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port: 유효하지 않은 값");
        }

        this.channel = createChannel(host, port);
        this.clientId = (clientId == null || clientId.isEmpty()) ? DEFAULT_ID : clientId;
    }

    private Channel createChannel(String host, int port) throws UnknownHostException, InterruptedException {
        Function<Integer, CompletableFuture<Message>> function = requests::get;
        BiFunction<String, Integer, BlockingQueue<Message>> biFunction = (topicName, partition) -> {
            Map<Integer, BlockingQueue<Message>> partitionMap = subscriptions.get(topicName);
            if (partitionMap == null) {
                return null;
            }

            return partitionMap.get(partition);
        };

        NettyInitializer initializer = NettyInitializer.builder()
            .addHandler(MessageDecoder.class)
            .addHandler(ClientInboundHandler.class, new Class<?>[] { Function.class, BiFunction.class }, function, biFunction)
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
        int requestId = requestCounter++;
        message.addOptions(Map.of(
            MessageOption.CLIENT_ID, clientId,
            MessageOption.REQUEST_ID, requestId
        ));

        /*
         * 결과 대기용 future 생성 및 
         * 서버 응답 도착 시 요청 맵에서 자동 제거하게 설정 후
         * 요청 맵에 추가
         */
        CompletableFuture<Message> future = new CompletableFuture<>();
        future.whenComplete((result, err) -> requests.remove(requestId));
        requests.put(requestId, future);

        // 메시지 전송
        channel.writeAndFlush(message);

        return future;
    }

    /**
     * 서버에 메시지 전송만 함(응답 무시)
     */
    public void command(Message message) {
        Utils.validate(message);
        message.addOption(MessageOption.CLIENT_ID, clientId);
        channel.writeAndFlush(message);
    }

    /**
     * 특정 토픽/파티션을 구독하고, 메시지가 업데이트되면 out 큐에 TOPIC_UPDATED를 포함한 메시지가 담김
     * 따라서, out 큐를 blocking하게 모니터링해서 TOPIC_UPDATED 메시지를 처리할 수 있음
     */
    public ExecutorService subscribe(String topicName, int partition, Collection<Message> out) {
        Utils.validate(topicName, out);

        Message request = Message.of(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.REQ_SUBSCRIBE.getByte(),
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        // 구독 요청
        Message response = request(request).join(); // 구독 요청 대기
        Byte messageType = response.optionAsByte(MessageOption.MESSAGE_TYPE);
        if (messageType == null || messageType != MessageType.RES_SUBSCRIBE.getByte()) {
            return null; // 구독 실패
        }
        
        /*
         * subscriptions 맵은 실제 메시지가 저장되는 토픽/파티션의 저장소와 별도의 공간으로
         * 저장소가 존재하지 않더라도 구독을 할 수 있음.(추후 추가되면 알림 수신 가능)
         * 이렇게 만든 이유는, 토픽/파티션이 아직 생성되지 않은 상태에서 구독 요청이 올 수 있기 때문
         */
        Map<Integer, BlockingQueue<Message>> partitionMap = subscriptions.computeIfAbsent(
            topicName, 
            ignored -> new ConcurrentHashMap<>()
        );

        BlockingQueue<Message> queue = partitionMap.computeIfAbsent(
            partition, 
            ignored -> new LinkedBlockingQueue<>()
        );

        // 별도의 스레드에서 알람 수신 대기
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            while (true) {
                try {
                    Message notified = queue.take(); // blocking
                    out.add(notified);
                } catch (Exception e) {
                    System.err.println("NettyClient.subscribe().executor 종료: " + e);
                    break;
                }
            }

            partitionMap.remove(partition); // 모니터링 종료 후 파티션 맵에서 제거
            unsubscribe(topicName, partition); // 구독 해제
        });

        return executor;
    }

    public void unsubscribe(String topicName, int partition) {
        Utils.validate(topicName);

        Message message = Message.of(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.REQ_UNSUBSCRIBE.getByte(),
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition
        ));

        // 구독 취소 요청
        command(message);
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
