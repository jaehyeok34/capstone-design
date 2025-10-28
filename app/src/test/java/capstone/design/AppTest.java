package capstone.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Optional;

import capstone.design.netty.NettyInitializer;
import org.junit.jupiter.api.Test;

class AppTest {

    @Test
    void nettyInitializerTest() {
        // handler를 하나도 등록하지 않으면 IllegalStateException 발생
        assertThrows(IllegalStateException.class, () -> {
            NettyInitializer.builder().build();
        });
    }

    @Test
    void optionalTest() {
        Optional<Object> o1 = Optional.ofNullable(null);
        assertThrows(NoSuchElementException.class, () -> o1.get());
    }

    @Test
    void byteTest() throws NumberFormatException, UnsupportedEncodingException {
        Object b = Byte.parseByte("10");

        byte b2 = Byte.parseByte(new String(b.toString().getBytes("utf-8")));

        assertEquals(b, b2);

        Object str = "안녕하세요";
        assertNotEquals(str.toString().length(), str.toString().getBytes("utf-8").length);

        byte[] bufs = new byte[] {1, 2, 3};
        Object ob = new byte[] {1, 2, 3};

        assertEquals(bufs.length, ob.toString().getBytes(StandardCharsets.UTF_8).length);

        // ByteBuf buf = Unpooled.buffer();
        // buf.writeBytes("123".getBytes("utf-8"));
        // Object b3 = buf;
        // assertEquals("123", new String(b3.toString().getBytes("utf-8")));
    }
}
