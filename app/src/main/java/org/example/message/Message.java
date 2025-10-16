package org.example.message;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;

// topic에 저장되는 데이터 통신 단위
public class Message {

    public static final int TOPIC_NAME_LENGTH = Integer.BYTES;

    private final String topicName;
    private final ByteBuf payload;

    private Message(Builder builder) { this(builder.topicName, builder.payload); }
    private Message(String topicName, ByteBuf payload) {
        this.topicName = topicName;
        this.payload = payload;
    }

    public static Message of(String topicName, ByteBuf payload) { return new Message(topicName, payload); }
    public static Message of(String topicName, byte[] payload) { return new Message(topicName, Unpooled.copiedBuffer(payload)); }
    public static Message of(String topicName) { return new Message(topicName, null); }
    public static Message of(ByteBuf payload) { return new Message(null, payload); }
    public static Message of(byte[] payload) { return new Message(null, Unpooled.copiedBuffer(payload)); }

    public ByteBuf toByteBuf() {
        if (topicName == null || topicName.isEmpty()) return payload;

        ByteBuf tBuf = Unpooled.buffer(TOPIC_NAME_LENGTH + topicName.length())
            .writeInt(topicName.length())
            .writeBytes(topicName.getBytes(StandardCharsets.UTF_8));

        if (payload == null) return tBuf;

        CompositeByteBuf buf = Unpooled.compositeBuffer();
        buf.addComponents(true, tBuf, payload);
        return buf;
    }

    public int getLength() { return (payload != null ? payload.readableBytes() : 0) + (topicName != null ? TOPIC_NAME_LENGTH + topicName.length() : 0); }
    public String getTopicName() { return topicName; }
    public byte[] getPayload() { return payload.array(); }
    public void release() { if (payload != null && payload.refCnt() > 0) payload.release(payload.refCnt()); }
    public ByteBuf retain() { return payload != null ? payload.retain() : null; }
    public boolean alivePayload() { return payload != null ? payload.refCnt() > 0 : false; }
    public static Builder builder() { return new Builder(); }
    
    public static class Builder {
        private String topicName;
        private ByteBuf payload;

        private Builder() {}

        public Builder topicName(String topicName) {
            if (topicName == null || topicName.isEmpty()) {
                throw new IllegalArgumentException("topicName: null 또는 빈 문자열");
            }

            this.topicName = topicName;
            return this;
        }

        public Builder payload(ByteBuf payload) {
            if (payload == null || payload.readableBytes() == 0) {
                throw new IllegalArgumentException("payload: null 또는 빈 ByteBuf");
            }

            this.payload = payload;
            return this;
        }

        public Builder payload(byte[] payload) {
            if (payload == null || payload.length == 0) {
                throw new IllegalArgumentException("payload: null 또는 빈 배열");
            }

            this.payload = Unpooled.copiedBuffer(payload);
            return this;
        }

        public Message build() { return new Message(this); }
    }
}
