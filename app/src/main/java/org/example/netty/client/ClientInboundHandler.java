package org.example.netty.client;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.example.message.MessageFrame;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private final Supplier<CompletableFuture<MessageFrame>> requests;

    public ClientInboundHandler(Supplier<CompletableFuture<MessageFrame>> requests) {
        if (requests == null) {
            throw new IllegalArgumentException("requests: null");
        }

        this.requests = requests;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("[debug] 서버 응답 수신");

        if (msg instanceof MessageFrame frame) {
            requests.get().complete(frame);
            System.out.println("[debug] future에 전달 완료");
        }
    }
}
