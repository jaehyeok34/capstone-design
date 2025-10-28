package capstone.design.topic.subscribe;

import capstone.design.Utils;
import io.netty.channel.ChannelHandlerContext;

public record Subscriber(ChannelHandlerContext context, String clientId) {

    public Subscriber {
        Utils.validate(context, clientId);
    }
}
