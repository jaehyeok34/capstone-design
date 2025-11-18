package capstone.design.netty.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import capstone.design.message.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

    private final Function<String, List<CompletableFuture<Message>>> requests;

    public ClientInboundHandler(Function<String, List<CompletableFuture<Message>>> requests) {
        this.requests = requests;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message message) {
            String requestId = message.header("request.id", "");
            if (requestId.isEmpty()) {
                return;
            }

            List<CompletableFuture<Message>> futures = requests.apply(requestId);
            if (futures == null || futures.isEmpty()) {
                return;
            }

            message.removeHeader("request.id");

            // 완료되지 않은 future를 찾아서 완료시킴
            for (CompletableFuture<Message> future : futures) {
                if (!future.isDone()) {
                    future.complete(message);
                    break;
                }
            }
        }
    }
}
