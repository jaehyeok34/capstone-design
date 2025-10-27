package org.example.spy;

import java.util.List;

import org.example.message.MessageDecoder;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class SpyMessageDecoder extends MessageDecoder {
    
    public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        super.decode(ctx, in, out);
    }
}
