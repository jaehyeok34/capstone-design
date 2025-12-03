package capstone.design.message;

import java.util.ArrayList;
import java.util.List;
import capstone.design.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
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
        
        Frame frame = message.toFrame();
        ByteBuf headerBuf = allocator.directBuffer()
            .writeInt(Utils.MAGIC)
            .writeLong(frame.length() + Integer.BYTES);

        out.add(headerBuf);
        out.add(frame.header());

        out.add(Unpooled.buffer().writeInt(frame.payloadLength()));
        if (frame.isPayload()) {
            out.add(frame.payload());
        }

        return out;
    }
}