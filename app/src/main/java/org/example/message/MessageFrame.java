package org.example.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public record MessageFrame(MessageHeader header, Message message) {

    /*
     * header는 반드시 존재해야 함
     * message는 REQ_PULL의 경우 null일 수 있음
     */
    public MessageFrame {
        if (header == null) throw new IllegalArgumentException("header: null");
    }

    public static ByteBuf ofByteBuf(MessageHeader header, Message message) {
        if (header == null || message == null) throw new IllegalArgumentException("header 또는 message: null");

        return Unpooled.wrappedBuffer(header.toByteBuf(), message.toByteBuf());
    }
}