package capstone.design.message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import capstone.design.Utils;
import capstone.design.topic.disk.DiskRecord;
import capstone.design.topic.memory.MemoryRecord;
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
        ByteBuf encoded = allocator.buffer();
        int totalLength = 0;

        for (Map.Entry<String, Object> entry : message.options().entrySet()) { 
            String key = entry.getKey();
            Object value = entry.getValue();

            // key 추가
            encoded.writeShort(key.length())
                .writeBytes(key.getBytes(StandardCharsets.UTF_8));

            // value 추가
            switch (value) {
                case DiskRecord record -> {
                    encoded.writeInt(record.length());
                    out.add(encoded);
                    out.add(record.value());

                    // 지금까지 기록한 옵션 길이 + record 길이 반영
                    totalLength += encoded.readableBytes() + record.length();
                    encoded = allocator.buffer();
                }
                case MemoryRecord record -> write(encoded, (byte[]) record.value());
                case byte[] buf -> write(encoded, buf);
                default -> write(encoded, value.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        out.add(encoded);

        totalLength += encoded.readableBytes();
        ByteBuf header = allocator.buffer()
            .writeInt(Utils.MAGIC)
            .writeLong(totalLength);
        out.addFirst(header);

        return out;
    }

    private void write(ByteBuf buf, byte[] data) {
        buf.writeInt(data.length).writeBytes(data);
    }
}