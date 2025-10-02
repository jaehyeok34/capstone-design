package org.example.message;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.example.message.MessageHeader.Type;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {
    /*
     * consumer -> broker: [type][request id][topic name length][topic name]
     * producer -> broker: [type][request id][topic name length][topic name][payload]
     * broker -> consumer: [type][request id][payload]
     * broker -> producer: X
     */

    private MessageHeader header;
    private Message message;
    private State state = State.READ_HEADER;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        while (true) {
            if (state == State.READ_HEADER) {
                if (in.readableBytes() < MessageHeader.HEADER_LENGTH) return;
    
                Type type = Type.values()[in.readByte()];
                int reqId = in.readInt();
                int msgLen = in.readInt();

                header = MessageHeader.of(type, reqId, msgLen);
                if (type == Type.RES_PULL && msgLen <= 0) {
                    out.add(new MessageFrame(header, null));
                    continue;
                }
                
                state = State.READ_PAYLOAD;
            } else {
                int msgLen = header.getMessageLength();
                if (msgLen <= 0) {
                    out.add(new MessageFrame(header, null));
                    state = State.READ_HEADER;
                    continue;
                }

                if (in.readableBytes() < msgLen) return;

                Type t = header.getType();
                if (t == Type.RES_PULL || t == Type.RES_PUSH) {
                    message = Message.of(in.readBytes(msgLen)); // only payload
                } else {
                    int nameLen = in.readInt();
                    String tName = in.readString(nameLen, StandardCharsets.UTF_8);
                    if (t == Type.REQ_PULL) message = Message.of(tName); // only topic name
                    else message = Message.of(tName, in.readBytes(msgLen - Message.TOPIC_NAME_LENGTH - nameLen));
                }

                if (message == null) throw new IllegalStateException("message: null");

                out.add(new MessageFrame(header, message));
                state = State.READ_HEADER;
            }
        }
    }

    private enum State { READ_HEADER, READ_PAYLOAD }

    public static record MessageFrame(MessageHeader header, Message message) {
        public static ByteBuf wrapToByteBuf(MessageHeader header, Message message) { return Unpooled.wrappedBuffer(header.toByteBuf(), message.toByteBuf()); }
    }
}