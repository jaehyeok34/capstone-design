package org.example.message;

import org.jspecify.annotations.NonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class Message {

    @NonNull private final ByteBuf payload;

    private Message(ByteBuf payload) { 
        if (payload == null || payload.readableBytes() == 0 || payload.refCnt() == 0) {
            throw new IllegalArgumentException("payload: null 또는 empty 또는 해제된 ByteBuf");
        }

        this.payload = payload; 
    }

    public static Message of(ByteBuf payload) { return new Message(payload); }
    public static Message of(byte[] payload) { return new Message(Unpooled.copiedBuffer(payload)); }

    public ByteBuf toByteBuf() { return payload; }
    public int length() { return payload.readableBytes(); }
    public void release() { payload.release(payload.refCnt()); }
    public ByteBuf retain() { return payload.retain(); }
}
