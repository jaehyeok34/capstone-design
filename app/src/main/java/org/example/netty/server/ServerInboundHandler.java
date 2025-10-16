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
        if (processor == null) throw new IllegalArgumentException("processor: null");

        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("[debug] 클라이언트 메시지 수신");

        if (msg instanceof MessageFrame frame) {
            MessageProcessor.Result result = processor.process(frame);
            if (result != null) {
                MessageHeader header = result.header();
                System.out.println("[debug] ServerInboundHandler.channelRead() - 메시지 길이: " + header.getMessageLength());

                Channel channel = ctx.channel();

                // ! 현재 result.message는 null 혹은 Record 타입 밖에 없지만, 향후 다른 타입이 추가될 수 있음
                channel.write(header.toByteBuf()); // header 전송
                if (result.message() instanceof TopicRecord record) { // payload 존재할 경우 전송
                    System.out.println("[debug] ServerInboundHandler.channelRead() - refCnt: " + record.getValue().refCnt());
                    channel.writeAndFlush(record.getValue());
                } else { // payload 없거나 Record가 아닌 경우 flush(header만 전송)
                    channel.flush();
                }
            } else {
                System.out.println("[debug] REQ_PUSH 처리 또는 에러");
            }
        }

        System.out.println("[debug] 메시지 처리 완료");
        super.channelRead(ctx, msg);

        System.out.println("[debug] payload 상태: " + (msg instanceof MessageFrame f ? f.message().alivePayload() : "X"));
    }
}
