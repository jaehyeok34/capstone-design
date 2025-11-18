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

            /*
             * 합성 future에서 아무 future나 꺼내서(삭제) 완료시킴
             * future가 모두 처리된다면, requests의 해당 id 항목은 빈 리스트가 될 것임
             */
            futures.remove(0).complete(message);
        }
    }
}
