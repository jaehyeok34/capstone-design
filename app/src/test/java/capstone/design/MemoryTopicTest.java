package capstone.design;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import capstone.design.spy.SpyContext;
import capstone.design.topic.memory.MemoryTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MemoryTopicTest {

    private MemoryTopic topic;
    private final int partition = 0;
    private final String clientId = "user";
    private final String message = "message";
    private final int messageLength = message.length();
    private final String topicName = "mem_topic";
    private final List<ByteBuf> bufs = new ArrayList<>();

    void addData() {
        for (int i = 0; i < 2; i++) {
            ByteBuf buf = Unpooled.copiedBuffer((message + i).getBytes());
            topic.push(partition, clientId + i, buf.retain());

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
        
        /*
         * 첫 번째 id는 2개의 메시지가 쌓여야함
         * partition에 대하여 push하게 되면, partition에 있는 id 전체에게 메시지가 쌓이기 때문에
         * 첫 번째 id는 2회 push(자신 + 다음), 두 번째 id는 1회 push(자신)이기 때문
         */
        assertEquals(2, topic.count(partition, clientId + 0));
        assertEquals(1, topic.count(partition, clientId + 1));

        // 없는 partition에 대해서 length 확인
        assertEquals(0, topic.count(partition + 1, clientId + 0));

        // 없는 id에 대해서 length 확인
        assertEquals(0, topic.count(partition, clientId + 2));
    }

    @Test
    void pullTest() {
        addData();

        /*
         * 커서 지정 시, 해당 커서 메시지 반환
         * 커서 미지정 시, FIFO로 동작
         */
        assertEquals(message + 1, ((ByteBuf) topic.pull(partition, clientId + 0, 1).value()).readString(messageLength + 1, StandardCharsets.UTF_8));
        assertEquals(message + 0, ((ByteBuf) topic.pull(partition, clientId + 0).value()).readString(messageLength + 1, StandardCharsets.UTF_8));
        
        /*
         * 동일 partition의 다른 id에는 pull 영향 없어야 하므로 1개 그대로 있어 함
         */
        assertEquals(1, topic.count(partition, clientId + 1));
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
