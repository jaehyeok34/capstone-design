package org.example.message;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

    private long length;
    private Message message;
    private State state = State.READ_MAGIC;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (true) {
            switch (state) {
                case READ_MAGIC -> { if (!readMagic(in)) return; }
                case READ_LENGTH -> { if (!readLength(in)) return; }
                case READ_MESSAGE -> { if (!readMessage(in, out)) return; }
                default -> {}
            }
        }
    }

    private boolean readMagic(ByteBuf in) {
        if (in.readableBytes() < Integer.BYTES) {
            return false;
        }

        while (true) {
            in.markReaderIndex(); // 현재 readerIndex 저장

            int magic = in.readInt();
            if (magic != MessageEncoder.MAGIC) {
                in.resetReaderIndex(); // readerIndex를 저장된 위치로 복원
                in.readByte(); // 1byte 버림
                continue;
            }

            state = State.READ_LENGTH;
            return true;
        }
     }

    private boolean readLength(ByteBuf in) {
        if (in.readableBytes() < Long.BYTES) {
            return false;
        }

        length = in.readLong();
        if (length > 0) {
            state = State.READ_MESSAGE;
        }

        return true;
    }

    private boolean readMessage(ByteBuf in, List<Object> out) {
        if (in.readableBytes() < length) {
            return false;
        }

        message = new Message();
        try {
            // type
            message.addOption(MessageOption.TYPE, in.readByte());

            // id
            String id = read(in, String.class);
            if (id != null) {
                message.addOption(MessageOption.ID, id);
            }

            // topic name
            String topicName = read(in, String.class);
            if (topicName != null) {
                message.addOption(MessageOption.TOPIC_NAME, topicName);
            }

            // partition
            if (in.readableBytes() < Integer.BYTES) {
                throw new IndexOutOfBoundsException();
            }
            message.addOption(MessageOption.PARTITION, in.readInt());

            // cursor
            if (in.readableBytes() < Long.BYTES) {
                throw new IndexOutOfBoundsException();
            }
            message.addOption(MessageOption.CURSOR, in.readLong());

            // payload
            ByteBuf payload = read(in, ByteBuf.class);
            if (payload != null) {
                message.addOption(MessageOption.PAYLOAD, payload);
            }
        } catch (IndexOutOfBoundsException ignored) {} 
        finally { // 데이터를 더이상 읽지 못하면 지금까지 읽은 message 반환
            out.add(message);
            state = State.READ_MAGIC;
        }

        return true;
    }

    /**
     * 유효한 데이터가 아니라 읽을 수 없다면 IndexOutOfBoundsException 발생
     *  1. readableBytes가 Integer.BYTES 만큼 없어서 length를 읽지 못하는 상황
     *  2. readableBytes가 length 만큼 없어서 데이터를 읽지 못하는 상황
     * 
     * 누락된 정보면 null 반환
     * 
     * 유효한 데이터면 해당 타입으로 반환
     */
    private <T> T read(ByteBuf buf, Class<T> type) throws IndexOutOfBoundsException {
        // length 읽을 수 있는지 판단
        if (buf.readableBytes() < Integer.BYTES) {
            throw new IndexOutOfBoundsException();
        }

        // length 읽기
        int length = buf.readInt();
        if (length <= 0) {
            return null; // 해당 정보 누락됨
        }

        // length 만큼 데이터를 읽을 수 있는지 판단
        if (buf.readableBytes() < length) {
            // throw new IndexOutOfBoundsException();
            return null;
        }

        // 데이터 읽기
        if (type == String.class) {
            return type.cast(buf.readString(length, StandardCharsets.UTF_8));
        } else if (type == ByteBuf.class) {
            return type.cast(buf.readBytes(length));
        }

        return null;
    }

    private enum State { READ_MAGIC, READ_LENGTH, READ_MESSAGE }
}