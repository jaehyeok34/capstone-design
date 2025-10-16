package org.example.message;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.example.message.MessageHeader.Type;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {
    /*
     * header: [type][request id][message length]
     * message: [topic name length][topic name]([payload])
     * consumer -> broker(REQ_PULL): [type][request id][message length] / [topic name length][topic name]
     * producer -> broker(REQ_PUSH): [type][request id][message length] / [topic name length][topic name][payload]
     * broker -> consumer(RES_PULL): [type][request id][message length] / [payload]
     * broker -> producer(RES_PUSH): X
     */

    private MessageHeader header;
    private Message.Builder builder;
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
                if (type == Type.REQ_PULL && msgLen <= 0) { // no payload
                    out.add(new MessageFrame(header, null));
                    continue;
                }
                
                state = State.READ_PAYLOAD;
            } else {
                int msgLen = header.getMessageLength();
                if (in.readableBytes() < msgLen) return;

                builder = Message.builder();

                Type type = header.getType();
                if (type == Type.RES_PULL) { // only payload
                    builder.payload(in.readBytes(msgLen));
                } else { // topic name + (payload)
                    int topicNameLength = in.readInt();
                    String topicName = in.readString(topicNameLength, StandardCharsets.UTF_8);

                    builder.topicName(topicName);

                    if (type == Type.REQ_PUSH) {
                        ByteBuf buf = in.readBytes(msgLen - Message.TOPIC_NAME_LENGTH - topicNameLength);
                        builder.payload(buf);
                    }
                }

                out.add(new MessageFrame(header, builder.build()));
                state = State.READ_HEADER;
            }
        }
    }

    private enum State { READ_HEADER, READ_PAYLOAD }
}