package capstone.design.message;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import capstone.design.Utils;
import capstone.design.topic.TopicRecord;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class MessageEncoder extends ChannelOutboundHandlerAdapter {
    
    private final static String PAYLOAD_TYPE = "5";
    
    private final String filePath;

    public MessageEncoder(String filePath) {
        Utils.validate(filePath);
        
        this.filePath = filePath;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message message) {
            encode(ctx.alloc(), message).forEach(ctx::write);
        }
    }

    public List<Object> encode(ByteBufAllocator allocator, Message message) throws IOException {
        List<Object> out = new LinkedList<>();
        Properties props = new Properties();
        try (FileReader reader = new FileReader(filePath)) {
            props.load(reader);
        }

        ByteBuf encoded = allocator.buffer().writeInt(Utils.MAGIC);
        ByteBuf in = allocator.buffer();
        Object payload = null;

        for (Map.Entry<String, Object> entry : message.options().entrySet()) {
            String key = entry.getKey();
            Object option = entry.getValue();
            if (!props.containsKey(key)) { // unknown option(TLVLV 형식)
                byte[] buf = option.toString().getBytes(StandardCharsets.UTF_8);
                in.writeByte(Byte.parseByte(Utils.UNKNOWN_OPTION_TYPE)) // type
                    .writeInt(key.length()) // key length
                    .writeBytes(key.getBytes(StandardCharsets.UTF_8)) // key value
                    .writeInt(buf.length) // value length
                    .writeBytes(buf); // value
                continue;
            }

            String optionType = props.getProperty(key);
            if (optionType.equals(PAYLOAD_TYPE)) { // payload 라면 lazy 처리
                payload = option;
                continue;
            } 

            writeOption(in, optionType, option); // TLV 형식
        }

        int weight = 0;
        if (payload != null) {
            in.writeByte(Byte.parseByte(PAYLOAD_TYPE)); // type

            /*
             * payload를 항상 마지막에 처리하는 이유는
             * payload가 TopicRecord일 때, 실제 구현체가 DiskTopic 이면 value()가 FileRegion이기 때문에
             * ByteBuf에 담을 수 없음. 따라서 length 정보만 담아두고 가중치 값 증가시켜 
             * message total length 계산 시 반영하도록 함
             */
            if (payload instanceof TopicRecord record) { 
                in.writeInt(record.length());
                weight = record.length();
                out.add(record.value());
            } else if (payload instanceof ByteBuf buf) {
                in.writeInt(buf.readableBytes()).writeBytes(buf);
            } else if (payload instanceof byte[] buf) {
                in.writeInt(buf.length).writeBytes(buf);
            } else { 
                in.writeInt(0);
            }
        }

        encoded.writeLong(in.readableBytes() + weight) // message total length
            .writeBytes(in); // message options

        out.addFirst(encoded);

        return out;
    }

    private void writeOption(ByteBuf in, String optionType, Object option) {
        byte[] buf = option.toString().getBytes(StandardCharsets.UTF_8);

        in.writeByte(Byte.parseByte(optionType)) // type
            .writeInt(buf.length) // length
            .writeBytes(buf); // value
    }
}