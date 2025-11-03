package capstone.design.message;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import capstone.design.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

    private long length;
    private State state = State.READ_MAGIC;

    private final String filePath;

    public MessageDecoder(String filePath) {
        Utils.validate(filePath);
        
        this.filePath = filePath;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        try {
            while (true) {
                switch (state) {
                    case READ_MAGIC -> { if (!readMagic(in)) return; }
                    case READ_LENGTH -> { if (!readLength(in)) return; }
                    case READ_MESSAGE -> { if (!readMessage(in, out)) return; }
                    default -> {}
                }
            }
        } catch (Exception e) { 
            System.err.println("MessageDecoder.decode 예외 발생: " + e); 

            /*
             * 디코딩 중 예외가 발생하면 지금까지 읽은 데이터 버리고 다음 메시지부터 다시 디코딩
             */
            length = 0;
            state = State.READ_MAGIC;

            System.out.println("[debug] MessageDecoder 에러 이후 채널 상태: " + ctx.channel().toString() + ", activce: " + ctx.channel().isActive());
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
                in.readByte(); // 1byte 버림
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

        this.length = in.readLong();
        this.state = State.READ_MESSAGE;
        
        return true;
    }

    public boolean readMessage(ByteBuf in, List<Object> out) throws Exception {
        return readMessage(this.length, in, out);
    }

    public boolean readMessage(long length, ByteBuf in, Collection<Object> out) throws Exception {
        if (in.readableBytes() < length) {
            return false;
        }

        long offset = 0;
        Properties props = new Properties();
        try (FileReader reader = new FileReader(filePath)) {
            props.load(reader);
        }
        props = invertProperties(props);

        Message message = new Message();
        while (offset < length) {
            byte optionType = in.readByte();
            offset += 1;

            if (optionType == Byte.parseByte(Utils.UNKNOWN_OPTION_TYPE)) { // TLVLV 형식 처리
                offset += addUnknownOption(message, in);
                continue;
            }

            /*
             * encoder에서 option_mapping_table.properties에 없는 key는 unknown option으로 처리하므로
             * key가 없는 경우는 고려하지 않음
             */
            String key = props.getProperty(String.valueOf(optionType));
            int len = in.readInt();
            switch (key) {
                case MessageOption.PAYLOAD -> {
                    ByteBuf buf = in.readBytes(len);
                    message.addOption(key, buf);
                }
                
                default -> {
                    byte[] buf = new byte[len];
                    in.readBytes(buf);
                    Utils.castAdd(message, key, buf);
                }
            }

            offset += Integer.BYTES + len;
        }

        out.add(message);
        this.state = State.READ_MAGIC;
        return true;
    }

    private long addUnknownOption(Message message, ByteBuf in) {
        long offset = 0;
        int keyLength = in.readInt();
        String key = in.readString(keyLength, StandardCharsets.UTF_8);
        offset += Integer.BYTES + keyLength;

        int valueLength = in.readInt();
        byte[] buf = new byte[valueLength];
        in.readBytes(buf);
        offset += Integer.BYTES + valueLength;

        message.addOption(key, new String(buf, StandardCharsets.UTF_8));

        return offset;
    }

    private Properties invertProperties(Properties props) {
        Properties inverted = new Properties();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            inverted.setProperty(value, key);
        }

        return inverted;
    }

    private enum State { READ_MAGIC, READ_LENGTH, READ_MESSAGE }
}