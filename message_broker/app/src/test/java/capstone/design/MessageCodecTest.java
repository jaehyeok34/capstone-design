// package capstone.design;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.io.IOException;
// import java.nio.charset.StandardCharsets;
// import java.util.Arrays;
// import java.util.List;
// import java.util.Map;

// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// import capstone.design.message.Message;
// import capstone.design.message.MessageDecoder;
// import capstone.design.message.MessageEncoder;
// import capstone.design.topic.TopicRecord;
// import capstone.design.topic.memory.MemoryRecord;
// import io.netty.buffer.ByteBuf;
// import io.netty.buffer.ByteBufAllocator;
// import io.netty.buffer.Unpooled;

// public class MessageCodecTest {
    
//     MessageEncoder encoder;
//     MessageDecoder decoder;

//     @BeforeEach
//     void beforeEach() {
//         encoder = new MessageEncoder();
//         decoder = new MessageDecoder();
//     }

//     @AfterEach
//     void afterEach() {
//         encoder = null;
//         decoder = null;
//     }

//     @Test
//     void encodeTest() throws IOException {
//         Message msg = Message.of(Map.of(
//             "o1", 123,
//             "o2", "456"
//         ));

//         List<Object> encoded1 = encoder.encode(ByteBufAllocator.DEFAULT, msg);

//         /*
//          * TopicRecord가 포함되지 않는 메시지의 경우
//          * 내부적으로 하나의 ByteBuf에 모든 옵션이 기록되어 List에 전달되기 때문
//          * 따라서 header buf + option buf = 2개
//          */
//         assertEquals(2, encoded1.size());

//         TopicRecord record = new MemoryRecord("789".getBytes(StandardCharsets.UTF_8));
//         msg.addOption("payload", record);

//         List<Object> encoded2 = encoder.encode(ByteBufAllocator.DEFAULT, msg);

//         /*
//          * TopicRecord가 포함되었기 때문에
//          * 내부적으로 ByteBuf에 옵션들을 list에 추가하고,
//          * record를 처리한 다음에
//          * 새로운 ByteBuf에 마저 옵션을 저장하고, list에 최종 추가하기 때문
//          * 따라서 header buf + option buf 1 + record buf + option buf 2 = 4개
//          */
//         assertEquals(4, encoded2.size());

//         ByteBuf buf2 = (ByteBuf) encoded2.getFirst();

//         /*
//          * key length(2) + key(2, "o1") + value length(4) + value(3, "123")
//          * + key length(2) + key(2, "o2") + value length(4) + value(3, "456")
//          * + key length(2) + key(7, "payload") + value length(4) + value(3, "789")
//          * = 38
//          */
//         assertEquals(38, buf2.skipBytes(4).readLong()); // total length
//         }

//     @Test
//     void decodeTest() throws Exception {
//         Message msg = Message.of(Map.of(
//             "o1", 123,
//             "o2", "456",
//             "o3", new byte[] {7, 8, 9}
//         ));

//         List<Object> encoded = encoder.encode(ByteBufAllocator.DEFAULT, msg);
//         ByteBuf buf = Unpooled.wrappedBuffer(
//             (ByteBuf) encoded.get(0), (ByteBuf) encoded.get(1)
//         );
//         Message decoded = decoder.decode(buf);

//         assertEquals(3, decoded.options().size());
//         assertEquals(123, Integer.parseInt(new String((byte[]) decoded.option("o1"), StandardCharsets.UTF_8)));
//         assertEquals("456", new String((byte[]) decoded.option("o2"), StandardCharsets.UTF_8));
//         assertTrue(Arrays.equals(new byte[] {7, 8, 9}, (byte[]) decoded.option("o3")));
//     }
// }
