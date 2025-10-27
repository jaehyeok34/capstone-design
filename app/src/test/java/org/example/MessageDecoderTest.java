package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.example.message.Message;
import org.example.message.MessageEncoder;
import org.example.message.MessageOption;
import org.example.spy.SpyMessageDecoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MessageDecoderTest {

    SpyMessageDecoder decoder;
    ByteBuf in;
    List<Object> out;

    @BeforeEach
    void beforeEach() {
        decoder = new SpyMessageDecoder();
        in = Unpooled.buffer();
        out = new ArrayList<>();
    }

    @AfterEach
    void afterEach() {
        decoder = null;

        out.clear();
        out = null;

        if (in.refCnt() > 0) {
            in.release();
        } 
    }

    @Test
    void withEmptyBuf() throws Exception {
        decoder.decode(null, in, out);

        // 아무 데이터도 없으니까 readMagic에서 return false가 되므로 out도 비어 있어야 함(무한 반복 X)
        assertEquals(0, out.size()); 
    }

    @Test
    void withMagic() throws Exception {
        in.writeByte(0).writeInt(MessageEncoder.MAGIC); // 쓰레기 값 + magic
        decoder.decode(null, in, out);

        /*
         * 매직 값이 포함되어 있으므로 magic은 통과하지만 길이 정보가 없으므로
         * readLength에서 return false가 되어 out도 비어 있어야 함
         * 단, 무한 반복이 걸려서도 안됨
         */
        assertEquals(0, out.size());
    }

    @Test
    void withMagicAndLength() throws Exception {
        in.writeInt(1234).writeInt(MessageEncoder.MAGIC).writeLong(10);
        decoder.decode(null, in, out);

        /*
         * 매직 값이 포함되어 있으므로 magic 통과
         * 길이 정보도 포함되어 있으므로 length도 통과
         * 하지만 실제 메시지가 없으므로 readMessage에서 return false가 되어 out도 비어 있어야 함
         */
        assertEquals(0, out.size());
    }

    @Test
    void withOneByteMessage() throws Exception {
        in.writeInt(1234) // 쓰레기 값
            .writeInt(MessageEncoder.MAGIC) // 매직
            .writeLong(1) // 길이 정보
            .writeByte(1); // 메시지

        decoder.decode(null, in, out);


        /*
         * 매직 값 포함: magic 통과
         * 길이 정보 포함: length 통과
         * 실제 메시지 1byte 포함: readMessage 통과, out에 메시지 추가
         */
        assertEquals(1, out.size());
        assertEquals(1, (byte) ((Message) out.remove(0)).option(MessageOption.TYPE, Byte.class));
    }

    @Test
    void withIdMessage() throws Exception {
        in.writeInt(0) // 쓰레기 값
            .writeInt(MessageEncoder.MAGIC) // magic
            .writeLong(1 + Integer.BYTES + 5) // length
            .writeByte(0) // type
            .writeInt(4) // id length
            .writeBytes("user".getBytes(StandardCharsets.UTF_8)) // id
            .writeInt(10); // 쓰레기 값
            
        decoder.decode(null, in, out);
        assertEquals(1, out.size());

        Message msg = (Message) out.remove(0);
        assertEquals(0, msg.option(MessageOption.TYPE, Byte.class).byteValue());
        assertEquals("user", msg.option(MessageOption.ID, String.class));
        assertNull(msg.option(MessageOption.TOPIC_NAME));
    }
}
