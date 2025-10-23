package org.example.message;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

// 추가하면 좋을 것:
// magic(내가 정의한 프로토콜이 맞는지 확인하기 위한 값, ex: 0xCFFA...)
// version(프로토콜 버전)
// checksum(무결성 확인)
public class MessageHeader {

    public static final int TOPIC_NAME_LENGTH_SIZE = Integer.BYTES;
    public static final int PARTITION_SIZE = Integer.BYTES;
    public static final int MESSAGE_LENGTH_SIZE = Integer.BYTES;
    public static final int HEADER_LENGTH = (
        Type.SIZE + 
        TOPIC_NAME_LENGTH_SIZE + 
        PARTITION_SIZE + 
        MESSAGE_LENGTH_SIZE
    );

    private final Type type;
    private final String topicName;
    private final int partition;
    private final int messageLength;

    private MessageHeader(Builder builder) {
        type = builder.type;
        topicName = builder.topicName;
        partition = builder.partition;
        messageLength = builder.messageLength;
    }

    public ByteBuf toByteBuf() {
        return Unpooled.buffer(HEADER_LENGTH)
            .writeByte(type.getByte())
            .writeInt(topicName.length())
            .writeBytes(topicName.getBytes(StandardCharsets.UTF_8))
            .writeInt(partition)
            .writeInt(messageLength);
    }

    public Type type() { return type; }
    public String topicName() { return topicName; }
    public int partition() { return partition; }
    public int messageLength() { return messageLength; }

    public static Builder builder(Type type, String topicName) {         
        return new Builder(type, topicName);
    }

    public static enum Type {
        REQ_PULL, REQ_PUSH, RES_PULL, RES_PUSH;
        public byte getByte() { return (byte) this.ordinal(); }
        public static int SIZE = Byte.BYTES;
    }

    public static class Builder {
        private final Type type;
        private final String topicName;
        private int partition = Integer.MIN_VALUE;
        private int messageLength = -1;

        private Builder(Type type, String topicName) {
            if (type == null) {
                throw new IllegalStateException("type: null");
            }

            if (topicName == null || topicName.isEmpty()) {
                throw new IllegalStateException("topicName: null or empty");
            }

            this.type = type;
            this.topicName = topicName;
        }

        public MessageHeader build() {
            return new MessageHeader(this);
        }

        public Builder partition(int partition) {
            this.partition = partition;
            return this;
        }

        public Builder messageLength(int messageLength) {
            this.messageLength = messageLength;
            return this;
        }
    }
}
