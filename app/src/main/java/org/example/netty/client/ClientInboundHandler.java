package org.example.netty.client;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.example.message.Message;
import org.example.message.MessageFrame;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private final Function<Integer, CompletableFuture<Message>> getRequest;

    public ClientInboundHandler(Function<Integer, CompletableFuture<Message>> getRequest) {
        if (getRequest == null) throw new IllegalArgumentException("getRequest: null");

        this.getRequest = getRequest;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("[debug] 서버 응답 수신");

        if (msg instanceof MessageFrame frame) {
            CompletableFuture<Message> future = getRequest.apply(frame.header().getRequestId());
            if (future != null) future.complete(frame.message());

            System.out.println("[debug] future에 전달 완료");
        }

        super.channelRead(ctx, msg);
    }
}
