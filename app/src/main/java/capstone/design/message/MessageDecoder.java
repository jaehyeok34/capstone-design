package capstone.design.message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

import capstone.design.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

    private long length;
    private State state = State.READ_MAGIC;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            List<Message> messages = new ArrayList<>();
            while (true) {
                if (state == State.READ_MAGIC && !readMagic(in)) {
                    break;
                }

                if (state == State.READ_LENGTH && !readLength(in)) {
                    break;
                }

                if (state == State.READ_MESSAGE) {
                    Message message = readMessage(in);
                    if (message == null) {
                        break;
                    }

                    messages.add(message);
                }
            }

            if (!messages.isEmpty()) {
                out.add(messages);
            }
        } catch (Exception e) {
            System.err.println("? MessageDecoder.decode(): " + e); 

            // 디코딩 중 예외가 발생하면 지금까지 읽은 데이터 버리고 다음 메시지부터 다시 디코딩
            length = 0;
            state = State.READ_MAGIC;
        }
    }

    /**
     * channel handler가 아닌 곳에서 직접 메시지를 디코딩해야 할 때 사용
     */
    public Message decode(ByteBuf in) throws Exception {
        int magic = in.readInt();
        if (magic != Utils.MAGIC) {
            return null;
        }

        long length = in.readLong();

        return readMessage(length, in);
    }

    private boolean readMagic(ByteBuf in) {
        while (in.readableBytes() >= Integer.BYTES) {
            in.markReaderIndex(); // 현재 readerIndex 저장

            int magic = in.readInt();
            if (magic != Utils.MAGIC) {
                in.resetReaderIndex(); // readerIndex를 저장된 위치로 복원
                in.skipBytes(1); // 1byte 버림
                continue;
            }

            this.state = State.READ_LENGTH;
            return true;
        }

        return false;
     }

    private boolean readLength(ByteBuf in) {
        if (in.readableBytes() < Long.BYTES) {
            return false;
        }

        length = in.readLong();
        state = State.READ_MESSAGE;
        
        return true;
    }

    private @Nullable Message readMessage(ByteBuf in) throws Exception {
        return readMessage(length, in);
    }

    private @Nullable Message readMessage(long length, ByteBuf in) throws Exception {
        if (in.readableBytes() < length) {
            return null;
        }

        Message.Builder builder = Message.builder();
        
        // 메시지 타입 읽기
        byte type = in.readByte();
        builder.type(type);

        // 헤더 읽기
        byte headerCount = in.readByte();
        for (int i = 0; i < headerCount; i++) {
            short keyLength = in.readShort();
            String key = in.readString(keyLength, StandardCharsets.UTF_8);

            int valueLength = in.readInt();
            String value = in.readString(valueLength, StandardCharsets.UTF_8);

            builder.header(key, value);
        }

        // payload 읽기
        if (in.readableBytes() >= Integer.BYTES) {
            int payloadLength = in.readInt();
            if (payloadLength > 0 && in.readableBytes() >= payloadLength) {
                byte[] payload = new byte[payloadLength];
                in.readBytes(payload);

                builder.payload(payload);
            }
        }

        state = State.READ_MAGIC;

        return builder.build();
    }

    private enum State { READ_MAGIC, READ_LENGTH, READ_MESSAGE }
}