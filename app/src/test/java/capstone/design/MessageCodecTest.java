package capstone.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import capstone.design.message.Message;
import capstone.design.message.MessageDecoder;
import capstone.design.message.MessageEncoder;
import capstone.design.message.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class MessageCodecTest {
    
    MessageEncoder encoder;
    MessageDecoder decoder;
    Message msg;

    final String filePath = "../option_mapping_table.properties";

    @BeforeEach
    void beforeEach() {
        encoder = new MessageEncoder(filePath);
        decoder = new MessageDecoder(filePath);
        msg = new Message().addOptions(Map.of(
            "message_type", MessageType.REQ_PULL.getByte(),
            "client_id", "user",
            "topic_name", "topic",
            "partition", 0,
            "cursor", 12L,
            "offset", 34L,
            "remaining_count", 100L,
            "invalid_option", "hello world",
            "success", true,
            "payload", "hello world".getBytes()
        ));
        msg.addOption("request_id", 12345);
    }

    @AfterEach
    void afterEach() {
        encoder = null;
        decoder = null;
        msg = null;
    }

    @Test
    void encodeTest() throws IOException {
        ByteBuf encoded = (ByteBuf) encoder.encode(ByteBufAllocator.DEFAULT, msg).get(0);
        assertEquals(Utils.MAGIC, encoded.readInt());
        
        long length = encoded.readLong();
        
        System.out.println("Total length: " + length);
        System.out.println(encoded.readString(encoded.readableBytes(), StandardCharsets.UTF_8));        
    }

    @Test
    void decodeTest() throws Exception {
        ByteBuf encoded = (ByteBuf) encoder.encode(ByteBufAllocator.DEFAULT, msg).get(0);
        Message decoded = decoder.decode(encoded);

        // 디코딩된 메시지의 옵션 개수는 invalid_option 제외 9개여야 함
        assertEquals(10, decoded.options().size());

        assertEquals(MessageType.REQ_PULL.getByte(), decoded.option("message_type"));

        assertInstanceOf(String.class, decoded.option("client_id"));
        assertEquals("user", decoded.option("client_id"));

        assertInstanceOf(String.class, decoded.option("topic_name"));
        assertEquals("topic", decoded.option("topic_name"));

        assertInstanceOf(Integer.class, decoded.option("partition"));
        assertEquals(0, decoded.option("partition"));

        assertInstanceOf(Long.class, decoded.option("cursor"));
        assertEquals(12L, decoded.option("cursor"));

        assertInstanceOf(Long.class, decoded.option("offset"));
        assertEquals(34L, decoded.option("offset"));

        assertInstanceOf(Long.class, decoded.option("remaining_count"));
        assertEquals(100L, decoded.option("remaining_count"));

        assertInstanceOf(Boolean.class, decoded.option("success"));
        assertEquals(true, decoded.option("success"));

        assertInstanceOf(ByteBuf.class, decoded.option("payload"));
        assertEquals("hello world", decoded.option("payload", ByteBuf.class).readString("hello world".length(), StandardCharsets.UTF_8));

        assertInstanceOf(Integer.class, decoded.option("request_id"));
        assertEquals(12345, decoded.option("request_id"));

        if (encoded.refCnt() > 0) {
            encoded.release();
        }
    }
}
