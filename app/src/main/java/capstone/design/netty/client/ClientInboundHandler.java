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
        Message message = (msg instanceof Message m) ? m : null;
        if (message == null) {
            return;
        }

        Byte type = message.optionAsByte(MessageOption.MESSAGE_TYPE);
        Utils.validate(type);

            switch (MessageType.values()[type]) {
                case TOPIC_UPDATED -> {
                    String topicName = message.optionAsString(MessageOption.TOPIC_NAME);
                    if (topicName == null) { // 토픽 정보가 없으면 알림 X
                        return; 
                    }
                    
                    Integer partition = message.optionAsInt(MessageOption.PARTITION);
                    BlockingQueue<Message> queue = subscriptions.apply(topicName, partition);
                    if (queue == null) { // 구독 정보가 없으면 알림 X
                        return;
                    }

                    queue.add(message); // 알림 메시지 전달
                }

                default -> {
                    Integer requestId = message.optionAsInt(MessageOption.REQUEST_ID);
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
                    
                    /*
                     * client.request()에서 request id를 추가했고, 그러면서 future를 생성했는데
                     * 여기서 해당하는 future를 찾아서 완료 시켰으니까 request id의 쓸모를 다함
                     */
                    message.removeOptions(MessageOption.REQUEST_ID); // 요청 ID 제거
                    future.complete(message);
                }
            }
    }
}
