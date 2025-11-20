package capstone.design.message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import capstone.design.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.FileRegion;

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

        encoded.writeByte(message.type().getByte()) // 메시지 타입 추가
            .writeByte(message.header().size()); // header 개수 추가
        for (Map.Entry<String, String> header : message.header().entrySet()) {
            byte[] key = header.getKey().getBytes(StandardCharsets.UTF_8);
            byte[] value = header.getValue().getBytes(StandardCharsets.UTF_8);
            
            // header의 key, value 추가
            encoded.writeShort(key.length).writeBytes(key)
                .writeInt(value.length).writeBytes(value);
        }
        
        totalLength += encoded.readableBytes();
        out.add(encoded); // type, header

        totalLength += writePayload(out, allocator, message.payload()); // payload

        ByteBuf frameHeader = allocator.buffer()
            .writeInt(Utils.MAGIC) // magic
            .writeLong(totalLength); // total length

        out.addFirst(frameHeader);

        return out;
    }

    /**
     * 새로운 ByteBuf를 생성하여 data를 기록하고, 이를 out에 추가.
     * byte[], ByteBuf, FileRegion 타입 지원
     * 그 외의 타입의 경우 toString().getBytes()로 변환하여 기록
     * 
     * @param allocator ByteBuf를 생성하기 위한 allocator
     * @param payload 기록할 데이터
     * @return 기록된 데이터의 총 크기
     */
    private long writePayload(List<Object> out, ByteBufAllocator allocator, Object payload) {
        long weight = Integer.BYTES;
        ByteBuf encoded = allocator.buffer();
        
        switch (payload) {
            case byte[] buf -> {
                weight += buf.length;
                encoded.writeInt(buf.length).writeBytes(buf);
            }

            case ByteBuf buf -> {
                weight += buf.readableBytes();
                encoded.writeInt(buf.readableBytes()).writeBytes(buf);
            }

            case FileRegion region -> {
                weight += region.count();
                encoded.writeInt((int) region.count());

                out.add(encoded);
                out.add(region);
                encoded = null;
            }

            case null -> encoded.writeInt(0);

            default -> {
                byte[] buf = payload.toString().getBytes(StandardCharsets.UTF_8);

                weight += buf.length;
                encoded.writeInt(buf.length).writeBytes(buf);
            }
        }

        out.add(encoded);

        return weight;
    }
}