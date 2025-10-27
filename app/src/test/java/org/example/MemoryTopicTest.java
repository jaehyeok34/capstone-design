package org.example;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.example.spy.SpyContext;
import org.example.topic.memory.MemoryRecord;
import org.example.topic.memory.MemoryTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MemoryTopicTest {

    private MemoryTopic topic;
    private final int partition = 0;
    private final String id = "user";
    private final String message = "message";
    private final String topicName = "mem_topic";
    private final List<ByteBuf> bufs = new ArrayList<>();

    void addData() {
        for (int i = 0; i < 2; i++) {
            ByteBuf buf = Unpooled.copiedBuffer(message.getBytes());
            topic.push(partition, id + i, buf.retain());

            bufs.add(buf);
        }
    }

    @BeforeEach
    void beforeEach() {
        topic = new MemoryTopic(topicName);
    }

    @AfterEach
    void afterEach() {
        topic = null;
        bufs.forEach(buf -> { if(buf.refCnt() > 0) buf.release(); });
    }

    @Test
    void pushTest() {
        addData();
        
        // 각 id에 대해서 length 확인
        for (int i = 0; i < 2; i++) {
            assertEquals(1, topic.length(partition, id + i));
        }

        // 없는 partition에 대해서 length 확인
        assertEquals(0, topic.length(partition + 1, id));

        // 없는 id에 대해서 length 확인
        assertEquals(0, topic.length(partition, id + 2));
    }

    @Test
    void pullTest() {
        addData();

        // partition, id에 대해서 pull 확인
        if (topic.pull(partition, id + 0) instanceof MemoryRecord record) {
            // 값이 잘 꺼내졌는지 확인
            assertEquals(0, topic.length(partition, id + 0));

            // 값 확인
            if (record.value() instanceof ByteBuf buf) {
                String msg = buf.readString(message.length(), StandardCharsets.UTF_8);
                assertEquals(message, msg);
            }
        }

        // 꺼낸 id 외의 다른 id는 값이 잘 남아있는지 확인
        assertEquals(1, topic.length(partition, id + 1));
    }

    @Test
    void subscribeTest() {
        SpyContext context = new SpyContext();

        // 구독자 확인
        assertEquals(0, topic.subscriberManager().count(partition));

        // 구독 후 구독자수 확인
        topic.subscribe(context, partition, "s1");
        assertEquals(1, topic.subscriberManager().count(partition));

        // 없는 id로 구독 해제 시도
        assertDoesNotThrow(() -> topic.unsubscribe(partition, "s0"));
        assertEquals(1, topic.subscriberManager().count(partition));

        // 구독 해제 후 구독자수 확인
        topic.unsubscribe(partition, "s1");
        assertEquals(0, topic.subscriberManager().count(partition));
    }
}
