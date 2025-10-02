package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.example.broker.topic.DiskTopic;
import org.example.broker.topic.DiskTopic.FileGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DiskTopicTest {
    
    static final String NAME = "test";
    static final FileGroup FG = new FileGroup(NAME);    

    private final DiskTopic topic;
    public DiskTopicTest() { 
        topic = new DiskTopic(NAME); 
    }

    @BeforeAll
    static void setup() {
        try {
            Files.deleteIfExists(FG.log());
            Files.deleteIfExists(FG.offset());
            Files.deleteIfExists(FG.cursor());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(1)
    void push() throws IOException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("hello world".getBytes(StandardCharsets.UTF_8));
        topic.push(buf);

        ByteBuf buf2 = Unpooled.buffer();
        buf2.writeBytes("test message".getBytes(StandardCharsets.UTF_8));
        topic.push(buf2);

        // ByteBuf의 참조 카운트가 정상적으로 감소 했는지 확인
        assertEquals(0, buf.refCnt());
        assertEquals(0, buf2.refCnt());

        List<String> msg = new ArrayList<>();
        try (
            RandomAccessFile logRaf = new RandomAccessFile(FG.log().toFile(), "r");
            RandomAccessFile offsetRaf = new RandomAccessFile(FG.offset().toFile(), "r")
        ) {
            // 메시지 2개 읽기
            for (int i = 0; i < 2; i++) {
                // 헤더 해석(offset 획득 -> length 획득 -> 메시지 읽기)
                offsetRaf.seek(i * Long.BYTES); // 읽기 위치 이동
                long offset = offsetRaf.readLong();

                logRaf.seek(offset); // 읽기 위치 이동
                int length = logRaf.readInt(); // 메시지 길이 획득

                // 실제 메시지 읽기
                byte[] msgBuf = new byte[length];
                logRaf.readFully(msgBuf);
                msg.add(new String(msgBuf, StandardCharsets.UTF_8));
            }
        }

        assertEquals("hello world", msg.get(0));
        assertEquals("test message", msg.get(1));
    }

    @Test
    @Order(2)
    void size() {
        assertEquals(2, topic.size());
    }

    @Test
    @Order(3)
    void pull() throws IOException{
        List<String> targets = List.of("hello world", "test message");
        for (int i = 0; i < 2; i++) {
            FileRegion fr = topic.pull().region();

            // #######################################################
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WritableByteChannel wbc = Channels.newChannel(baos);

            fr.transferTo(wbc, 0);
            byte[] msg = baos.toByteArray();

            // 메시지 확인
            assertEquals(targets.get(i), new String(msg, StandardCharsets.UTF_8));
            
            // FileRegion을 해제(release)하기 이전 상태 확인
            assert(fr.refCnt() == 1); // 참조 카운트가 1이어야 하고

            fr.release(); // FileRegion 해제
            assert(fr.refCnt() == 0); // 참조 카운트가 감소되어 0이 되어야 함
    
            try {
                byte[] cursorBuf = Files.readAllBytes(FG.cursor());
                long cursor = ByteBuffer.wrap(cursorBuf).getLong();
    
                // cursor 값이 증가 했고, 저장 됐는지 확인
                assertEquals(i + 1, cursor);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
