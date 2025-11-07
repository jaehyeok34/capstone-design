package capstone.design.netty.server;

import capstone.design.message.MessageProcessor;
import capstone.design.Utils;
import capstone.design.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private final MessageProcessor processor;

    public ServerInboundHandler(MessageProcessor processor) {
        Utils.validate(processor);

        this.processor = processor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message message) {
            System.out.println("[debug] 서버 수신: " + message);
            processor.process(ctx, message);
            System.out.println("[debug] 서버 처리 완료: " + message);
        }
    }
}
