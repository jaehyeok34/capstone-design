package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.example.message.Message;
import org.example.message.MessageDecoder;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class MessageDecoderTest {

    class SpyMessageDecoder extends MessageDecoder {
        public void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            super.decode(ctx, in, out);
        }
    }

    SpyMessageDecoder decoder;

    @BeforeEach
    void beforeEach() {
        decoder = new SpyMessageDecoder();
    }

    @AfterEach
    void afterEach() {
        decoder = null;
    }

    @Test
    void decodeHeaderOnly() throws Exception {
        ByteBuf in = Unpooled.buffer()
            .writeByte(MessageHeader.Type.REQ_PULL.getByte());

        List<Object> out = new ArrayList<>();

        // type만 전달한 상황
        decoder.decode(null, in, out);
        assertEquals(0, out.size()); // 아직 header가 완전히 안들어왔으므로 out은 비어 있어야 함

        String topicName = "test_topic";
        in.writeInt(topicName.length())
            .writeBytes(topicName.getBytes(StandardCharsets.UTF_8))
            .writeInt(11)
            .writeInt(-1);

        decoder.decode(null, in, out);
        assertEquals(1, out.size()); // header가 완전히 들어왔으므로(only header) out에 추가되어야 함

        if (out.get(0) instanceof MessageFrame frame) {
            MessageHeader header = frame.header();
            assertNotNull(header);

            Message message = frame.message();
            assertNull(message);

            assertEquals(MessageHeader.Type.REQ_PULL, header.type());
            assertEquals(topicName, header.topicName());
            assertEquals(11, header.partition());
            assertEquals(-1, header.messageLength());
        }
    }

    @Test
    void decode() throws Exception {
        List<Object> out = new ArrayList<>();
        Message message = Message.of("hello world".getBytes(StandardCharsets.UTF_8));
        MessageHeader header = MessageHeader.builder(MessageHeader.Type.REQ_PUSH, "test_topic")
            .partition(1024)
            .messageLength(message.length())
            .build();

            ByteBuf in = header.toByteBuf();
            decoder.decode(null, in, out);
            assertEquals(0, out.size()); // 아직 payload가 안들어왔으므로 out은 비어 있어야 함

            in.writeBytes(message.toByteBuf());
            decoder.decode(null, in, out);
            assertEquals(1, out.size()); // payload가 완전히 들어왔으므로 out에 추가되어야 함

            if (out.get(0) instanceof MessageFrame frame) {
                MessageHeader h = frame.header();
                assertNotNull(h);

                Message m = frame.message();
                assertNotNull(m);

                assertEquals(header.type(), h.type());
                assertEquals(header.topicName(), h.topicName());
                assertEquals(header.partition(), h.partition());
                assertEquals(header.messageLength(), h.messageLength());

                assertEquals(11, m.length());
            }
    }
}
