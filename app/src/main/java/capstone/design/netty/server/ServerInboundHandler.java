package capstone.design.netty.server;

import capstone.design.message.MessageProcessor;

import java.util.concurrent.ExecutorService;
import capstone.design.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

    private final MessageProcessor processor;
    private final ExecutorService executor;

    public ServerInboundHandler(MessageProcessor processor, ExecutorService executor) {
        this.processor = processor;
        this.executor = executor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message message) {
            System.out.println("! 서버 수신: " + msg);
            executor.submit(() -> processor.process(ctx, message));
        }
    }
}
