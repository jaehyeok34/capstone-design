package org.example.netty.server;

import org.example.message.MessageProcessor;

import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.example.topic.TopicRecord;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private final MessageProcessor processor;

    public ServerInboundHandler(MessageProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("processor: null");
        }

        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("[debug] 클라이언트 메시지 수신");

        if (msg instanceof MessageFrame frame) {
            Channel channel = ctx.channel();
            MessageProcessor.Result result = processor.process(frame);
            
            MessageHeader header = result.header();
            channel.write(header.toByteBuf()); // header 전송

            Object message = result.message();
            if (message instanceof TopicRecord record) {
                channel.writeAndFlush(record.value()); // payload 전송
                record.release(); // topic에서 꺼내고, 전송까지 했으므로 해제
            } else { channel.flush(); } // payload가 없으면 header만 전송
        }

        System.out.println("[debug] 메시지 처리 완료");
    }
}
