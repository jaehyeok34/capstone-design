package org.example.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MessageHeader {

    public static final int MESSAGE_TYPE_SIZE = Byte.BYTES;
    public static final int REQUEST_ID_SIZE = Integer.BYTES;
    public static final int MESSAGE_LENGTH_SIZE = Integer.BYTES;
    public static final int HEADER_LENGTH = MESSAGE_TYPE_SIZE + REQUEST_ID_SIZE + MESSAGE_LENGTH_SIZE;

    // magic(내가 정의한 프로토콜이 맞는지 확인하기 위한 값, ex: 0xCFFA...)
    // version(프로토콜 버전)
    // checksum(무결성 확인)
    private final Type type;
    private final int requestId;
    private final int messageLength;

    private MessageHeader(Type type, int requestId, int messageLength) {
        if (type == null) throw new IllegalArgumentException("messageType: null");

        this.type = type;
        this.requestId = requestId;
        this.messageLength = messageLength;
    }

    public static MessageHeader of(Type type, int requestId, int messageLength) {  return new MessageHeader(type, requestId, messageLength); }
    public static MessageHeader of(Type type, int requestId) { return new MessageHeader(type, requestId, -1); }

    public ByteBuf toByteBuf() {
        return Unpooled.buffer(HEADER_LENGTH)
            .writeByte(type.getByte())
            .writeInt(requestId)
            .writeInt(messageLength);
    }

    public Type getType() { return type; }
    public int getRequestId() { return requestId; }
    public int getMessageLength() { return messageLength; }

    public static enum Type {
        REQ_PULL, REQ_PUSH, RES_PULL, RES_PUSH;
        public byte getByte() { return (byte) this.ordinal(); }
    }
}
