package capstone.design;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;

import capstone.design.message.Message;
import capstone.design.message.MessageType;

class AppTest {

    @Test
    void test() {
        Message msg = Message.builder()
            .type(MessageType.REQ_PUSH)
            .header("int", "10")
            .header("long", "20")
            .build();

        assertInstanceOf(Long.class, msg.header("long", 0L));
        assertInstanceOf(Integer.class, msg.header("int", 0));
    }
}
