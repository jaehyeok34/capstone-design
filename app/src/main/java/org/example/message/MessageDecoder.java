package org.example.message;

import java.nio.charset.StandardCharsets;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {
    /*
     * header: [type][topic name length][topic name][partition][message length]
     * message: [payload]
     * 
     * consumer -> broker(REQ_PULL): header only
     * producer -> broker(REQ_PUSH): header + payload
     * broker -> consumer(RES_PULL): header + payload
     * broker -> producer(RES_PUSH): X
     */

    private MessageHeader header;
    private Message message;
    private State state = State.READ_HEADER;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (true) {
            if (state == State.READ_HEADER) {
                if (in.readableBytes() < MessageHeader.HEADER_LENGTH) return;

                MessageHeader.Type type = MessageHeader.Type.values()[in.readByte()];
                int topicNameLength = in.readInt();
                String topicName = in.readString(topicNameLength, StandardCharsets.UTF_8);

                header = MessageHeader.builder(type, topicName)
                    .partition(in.readInt())
                    .messageLength(in.readInt())
                    .build();

                if (header.messageLength() <= 0) { // only header
                    out.add(MessageFrame.of(header));
                    return;
                }

                state = State.READ_PAYLOAD;
            } else {
                int messageLength = header.messageLength();

                if (in.readableBytes() < messageLength) return; // payload는 있지만 아직 다 안들어 옴

                ByteBuf payload = in.readBytes(messageLength);
                message = Message.of(payload);
                
                out.add(MessageFrame.of(header, message));
                state = State.READ_HEADER;
            }
        }
    }

    private enum State { READ_HEADER, READ_PAYLOAD }
}