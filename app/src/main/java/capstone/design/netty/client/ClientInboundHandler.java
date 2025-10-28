package capstone.design.netty.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private final Function<Integer, CompletableFuture<Message>> requests; // RES_XXX 처리 용
    private final BiFunction<String, Integer, BlockingQueue<Message>> subscriptions; // TOPIC_UPDATED 알림 용

    public ClientInboundHandler(
        Function<Integer, CompletableFuture<Message>> requests,
        BiFunction<String, Integer, BlockingQueue<Message>> subscriptions
    ) {
        Utils.validate(requests, subscriptions);

        this.requests = requests;
        this.subscriptions = subscriptions;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("[debug] 서버 응답 수신");

        if (msg instanceof Message message) {
            Byte type = message.option(MessageOption.TYPE, Byte.class);
            
            Utils.validate(type);

            switch (MessageType.values()[type]) {
                case TOPIC_UPDATED -> {
                    String topicName = message.option(MessageOption.TOPIC_NAME, String.class);
                    if (topicName == null) { // 토픽 정보가 없으면 알림 X
                        return; 
                    }
                    
                    int partition = message.option(MessageOption.PARTITION, Integer.class);
                    BlockingQueue<Message> queue = subscriptions.apply(topicName, partition);
                    if (queue == null) { // 구독 정보가 없으면 알림 X
                        return;
                    }

                    queue.add(message); // 알림 메시지 전달
                }

                default -> {
                    Integer requestId = message.option(MessageOption.REQUEST_ID, Integer.class);
                    if (requestId == null) {
                        /*
                         * RES_XXX 메시지인데, 요청 ID가 없다는 것은
                         * client.request()가 아닌 command()로 보냈다는 뜻.
                         * 즉, 응답을 기다리지 않기 때문에 무시해도 됨
                         */
                        return;
                    }

                    CompletableFuture<Message> future = requests.apply(requestId);
                    if (future == null) {
                        return;
                    }

                    future.complete(message);
                }
            }

            System.out.println("[debug] 서버 응답 처리 완료");
        }
    }
}
