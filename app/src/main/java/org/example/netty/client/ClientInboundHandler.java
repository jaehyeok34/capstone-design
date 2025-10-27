package org.example.netty.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.example.Utils;
import org.example.message.Message;
import org.example.message.MessageOption;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private final Supplier<CompletableFuture<Message>> requests; // RES_XXX 처리 용
    private final BiFunction<String, Integer, BlockingQueue<Message>> subscriptions; // TOPIC_UPDATE 알림 용

    public ClientInboundHandler(
        Supplier<CompletableFuture<Message>> requests,
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

            switch (Message.Type.values()[type]) {
                case TOPIC_UPDATE -> {
                    String topicName = message.option(MessageOption.TOPIC_NAME, String.class);
                    Integer partition = message.option(MessageOption.PARTITION, Integer.class);
                    if (topicName == null || partition == null) { // 토픽/파티션 정보가 없으면 알림 X
                        return; 
                    }

                    BlockingQueue<Message> queue = subscriptions.apply(topicName, partition);
                    if (queue == null) { // 구독 정보가 없으면 알림 X
                        return;
                    }

                    queue.add(message); // 알림 메시지 전달
                }

                default -> {
                    CompletableFuture<Message> future = requests.get();
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
