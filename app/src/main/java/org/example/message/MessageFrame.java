package org.example.message;

public record MessageFrame(MessageHeader header, Message message) {

    /*
     * header는 반드시 존재해야 함
     * message는 경우에 따라 null일 수 있음
     */
    public MessageFrame {
        if (header == null) throw new IllegalArgumentException("header: null");
    }

    public static MessageFrame of(MessageHeader header) { return new MessageFrame(header, null); }
    public static MessageFrame of(MessageHeader header, Message message) { return new MessageFrame(header, message);}
}