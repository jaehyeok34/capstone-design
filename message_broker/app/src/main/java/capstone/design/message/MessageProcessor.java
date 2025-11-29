package capstone.design.message;

import io.netty.channel.ChannelHandlerContext;

public interface MessageProcessor {
    void process(ChannelHandlerContext context, Message message);
}
