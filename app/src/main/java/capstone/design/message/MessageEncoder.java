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

        if (message instanceof Message msg) {
            Properties props = new Properties();
            try (FileReader reader = new FileReader(filePath)) {
                props.load(reader);
            }

            ByteBuf encoded = allocator.buffer().writeInt(Utils.MAGIC);
            ByteBuf options = allocator.buffer();
            Object payload = null;

            for (Map.Entry<String, Object> entry : msg.options().entrySet()) {
                String key = entry.getKey();
                if (!props.containsKey(key)) {
                    System.out.println("[debug] skip unknown option key: " + key);
                    continue;
                }

                Object option = entry.getValue();
                String mappedKey = props.getProperty(key);
                
                if (mappedKey.equals("5")) { // payload 라면 lazy 처리
                    payload = option;
                    continue;
                }

                mappingAndWriteOption(options, mappedKey, option);
            }

            int weight = 0;
            if (payload != null) {
                options.writeByte(5); // type
                
                /*
                 * payload를 항상 마지막에 처리하는 이유는
                 * payload가 TopicRecord일 때, 실제 구현체가 DiskTopic 이면 value()가 FileRegion이기 때문에
                 * ByteBuf에 담을 수 없음. 따라서 length 정보만 담아두고 가중치 값 증가시켜 
                 * message total length 계산 시 반영하도록 함
                 */
                if (payload instanceof TopicRecord record) { 
                    options.writeInt(record.length());
                    weight = record.length();
                    out.add(record.value());
                } else if (payload instanceof ByteBuf buf) {
                    options.writeInt(buf.readableBytes()).writeBytes(buf);;
                } else if (payload instanceof byte[] buf) {
                    options.writeInt(buf.length).writeBytes(buf);
                } else { 
                    options.writeInt(0);
                }
            }

            encoded.writeLong(options.readableBytes() + weight) // message total length
                .writeBytes(options); // message options

            out.addFirst(encoded);
        }

        return out;
    }

    private void mappingAndWriteOption(ByteBuf in, String key, Object option) {
        in.writeByte(Byte.parseByte(key)); // type

        switch (key) {
            // message_type(byte)
            case "0" -> in.writeByte((byte) option);

            // success(byte)
            case "8" -> in.writeByte((boolean) option ? 1 : 0);

            // int, long, String, ... 
            default -> writeObject(in, option);
        } 
    }

    private void writeObject(ByteBuf in, Object option) {
        byte[] buf = option.toString().getBytes(StandardCharsets.UTF_8);

        in.writeInt(buf.length) // length
            .writeBytes(buf); // value
    }
}