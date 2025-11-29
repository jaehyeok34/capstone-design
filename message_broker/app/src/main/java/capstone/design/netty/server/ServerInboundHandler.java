package capstone.design.netty.server;

import capstone.design.message.MessageProcessor;

import java.util.List;
import capstone.design.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {
    private final MessageProcessor processor;

    public ServerInboundHandler(MessageProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ((List<?>) msg).forEach(message -> {
            processor.process(ctx, (Message) message);
        });
    }
}
