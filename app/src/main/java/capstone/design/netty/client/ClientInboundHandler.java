package capstone.design.netty.client;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import capstone.design.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private final Function<String, CompletableFuture<Message>> requests;

    public ClientInboundHandler(Function<String, CompletableFuture<Message>> requests) {
        this.requests = requests;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message message) {
            String requestId = message.header("request.id", "");
            if (requestId.isEmpty()) {
                return;
            }

            CompletableFuture<Message> future = requests.apply(requestId);
            if (future == null) {
                return;
            }

            message.removeHeader("request.id");
            future.complete(message);
        }
    }
}
