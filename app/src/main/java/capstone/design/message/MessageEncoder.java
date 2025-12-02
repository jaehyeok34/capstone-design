package capstone.design.message;

import java.util.ArrayList;
import java.util.List;
import capstone.design.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class MessageEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message message) {
            encode(ctx.alloc(), message).forEach(ctx::write);
        }
    }

    public List<Object> encode(ByteBufAllocator allocator, Message message) {
        List<Object> out = new ArrayList<>();
        
        // ByteBuf messageBuf = message.toByteBuf();
        Frame frame = message.toFrame();
        ByteBuf headerBuf = allocator.directBuffer()
            .writeInt(Utils.MAGIC)
            .writeLong(frame.length());

        out.add(headerBuf);
        out.add(frame.toByteBuf());

        if (frame.isPayloadFileRegion()) {
            out.add(frame.payload());
        }

        return out;
    }
}