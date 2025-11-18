package capstone.design.message;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
            while (true) {
                switch (state) {
                    case READ_MAGIC -> { if (!readMagic(in)) return; }
                    case READ_LENGTH -> { if (!readLength(in)) return; }
                    case READ_MESSAGE -> { if (!readMessage(in, out)) return; }
                }
            }
        } catch (Exception e) { 
            System.err.println("! MessageDecoder.decode(): " + e); 

            // 디코딩 중 예외가 발생하면 지금까지 읽은 데이터 버리고 다음 메시지부터 다시 디코딩
            length = 0;
            state = State.READ_MAGIC;
            System.out.println("! MessageDecoder 에러 이후 채널 상태: " + ctx.channel().toString() + ", activce: " + ctx.channel().isActive());
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
        BlockingQueue<Object> out = new LinkedBlockingQueue<>();

        if(!readMessage(length, in, out)) {
            return null;
        }

        return (Message) out.take();
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

    private boolean readMessage(ByteBuf in, List<Object> out) throws Exception {
        return readMessage(length, in, out);
    }

    private boolean readMessage(long length, ByteBuf in, Collection<Object> out) throws Exception {
        if (in.readableBytes() < length) {
            return false;
        }

        Message.Builder builder = Message.builder();
        
        // 메시지 타입 읽기
        byte type = in.readByte();
        builder.type(type);

        // 헤더 읽기
        byte headerCount = in.readByte();
        for (int i = 0; i < headerCount; i++) {
            byte keyLength = in.readByte();
            String key = in.readString(keyLength, StandardCharsets.UTF_8);

            int valueLength = in.readInt();
            String value = in.readString(valueLength, StandardCharsets.UTF_8);

            builder.header(key, value);
        }

        // payload 읽기
        if (in.readableBytes() > 0) {
            int payloadLength = in.readInt();
            byte[] payload = new byte[payloadLength];
            in.readBytes(payload);

            builder.payload(payload);
        }

        out.add(builder.build());
        state = State.READ_MAGIC;

        return true;
    }

    private enum State { READ_MAGIC, READ_LENGTH, READ_MESSAGE }
}