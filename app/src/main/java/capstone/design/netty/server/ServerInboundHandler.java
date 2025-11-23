package capstone.design.netty.server;

import capstone.design.message.MessageProcessor;

import java.util.List;
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
        List<?> messages = (List<?>) msg;

        for (Object message : messages) {
            Message m = (Message) message;
            executor.submit(() -> processor.process(ctx, m));
        }
    }
}
