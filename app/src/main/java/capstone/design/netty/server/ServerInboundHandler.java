package capstone.design.netty.server;

import capstone.design.message.MessageProcessor;
import capstone.design.Utils;
import capstone.design.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<Message> ATTRIBUTE_KEY = AttributeKey.valueOf("message");
    private final MessageProcessor processor;

    public ServerInboundHandler(MessageProcessor processor) {
        Utils.validate(processor);

        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message message) {
            System.out.println("! 서버 수신: " + msg);
            ctx.channel().attr(ATTRIBUTE_KEY).set(message);
            processor.process(ctx, message);
        }
    }
}
