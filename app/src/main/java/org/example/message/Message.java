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

    private Message(ByteBuf payload) {
        validate(payload);

        this.topicName = null;
        this.payload = payload;
    }

    private Message(String topicName) {
        validate(topicName);

        this.topicName = topicName;
        this.payload = null;
    }
    
    private Message(String topicName, ByteBuf payload) {
        validate(topicName);
        validate(payload);

        this.topicName = topicName;
        this.payload = payload;
    }

    public static Message of(String topicName, ByteBuf payload) { return new Message(topicName, payload); }
    public static Message of(String topicName, byte[] payload) { return new Message(topicName, Unpooled.copiedBuffer(payload)); }
    public static Message of(String topicName) { return new Message(topicName); }
    public static Message of(ByteBuf payload) { return new Message(payload); }
    public static Message of(byte[] payload) { return new Message(Unpooled.copiedBuffer(payload)); }

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
    public void release() { if (payload != null && payload.refCnt() > 0) payload.release(payload.refCnt()); }
    public String getTopicName() { return topicName; }
    public ByteBuf getPayload() { return payload; }
    public boolean isPayload() { return payload != null; }

    private void validate(String topicName) { if (topicName == null || topicName.isEmpty()) throw new IllegalArgumentException("topicName: null 또는 비어있음"); }
    private void validate(ByteBuf payload) { if (payload == null || payload.readableBytes() == 0) throw new IllegalArgumentException("payload: null 또는 비어있음"); }
}
