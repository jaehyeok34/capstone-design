package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.example.topic.TopicRecord;
import org.example.topic.disk.DiskTopic;
import org.example.topic.disk.DiskTopic.FileGroup;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DiskTopicTest {
    
    static final String NAME = "test";

    private final DiskTopic topic;

    public DiskTopicTest() throws IOException { 
        topic = new DiskTopic(NAME); 
    }

    @BeforeAll
    void setup() {
        FileGroup fg = topic.getFileGroup();
        try {
            Files.deleteIfExists(fg.log());
            Files.deleteIfExists(fg.offset());
            Files.deleteIfExists(fg.cursor());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    @Order(1)
    void push() throws IOException {
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("hello world".getBytes(StandardCharsets.UTF_8));
        topic.push(buf.retain());

        ByteBuf buf2 = Unpooled.buffer();
        buf2.writeBytes("test message".getBytes(StandardCharsets.UTF_8));
        topic.push(buf2.retain());

        // ByteBuf의 참조 카운트가 정상적으로 감소 했는지 확인
        assertEquals(1, buf.refCnt());
        assertEquals(1, buf2.refCnt());

        FileGroup fg = topic.getFileGroup();
        List<String> msg = new ArrayList<>();
        try (
            RandomAccessFile logFile = new RandomAccessFile(fg.log().toFile(), "r");
            RandomAccessFile offsetFile = new RandomAccessFile(fg.offset().toFile(), "r")
        ) {
            // 메시지 2개 읽기
            for (int i = 0; i < 2; i++) {
                // 헤더 해석(offset 획득 -> length 획득 -> 메시지 읽기)
                offsetFile.seek(i * Long.BYTES); // 읽기 위치 이동
                long offset = offsetFile.readLong();

                logFile.seek(offset); // 읽기 위치 이동
                int length = logFile.readInt(); // 메시지 길이 획득

                // 실제 메시지 읽기
                byte[] msgBuf = new byte[length];
                logFile.readFully(msgBuf);
                msg.add(new String(msgBuf, StandardCharsets.UTF_8));
            }
        }

        assertEquals("hello world", msg.get(0));
        assertEquals("test message", msg.get(1));
    }

    @Test
    @Order(2)
    void getLength() {
        assertEquals(2, topic.getLength());
    }

    @Test
    @Order(3)
    void pull() throws IOException{
        List<String> targets = List.of("hello world", "test message");
        for (int i = 0; i < 2; i++) {
            TopicRecord record = topic.pull();
            assertNotNull(record);
            assertEquals(2, topic.getLength());
            assertEquals(i + 1, topic.getCursor());

            // 꺼낸 데이터와 검증 데이터의 길이가 같은지(내용이 동일한지) 비교
            assertEquals(targets.get(i).length(), record.getLength());
        }
    }
}
