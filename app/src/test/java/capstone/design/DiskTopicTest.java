package capstone.design;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

import capstone.design.topic.TopicRecord;
import capstone.design.topic.disk.DiskTopic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;

public class DiskTopicTest {

    final String topicName = "test_topic";
    DiskTopic topic;

    void addData(int partition, String msg) {
        ByteBuf buf = Unpooled.copiedBuffer((msg).getBytes(StandardCharsets.UTF_8));
        topic.push(partition, null, buf);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        topic = new DiskTopic(topicName, 0, 3000);
    }

    @AfterEach
    void afterEach() throws IOException {
        topic.clearAll();
    }

    @Test
    void pushTest() throws InterruptedException {
        addData(0, "msg");
        addData(0, "msg1");
        topic.segmentManager(0).rollover(System.currentTimeMillis());

        /*
         * segment duration을 0ms로 설정 즉, 매번 새로운 세그먼트 생성하기 때문에
         * 2개의 메시지를 저장했으므로 세그먼트는 2개지만
         * 명시적으로 rollover()를 호출했기 때문에 총 3개가 됨
         */
        assertEquals(3, topic.segmentCount(0));
        assertEquals(2, topic.count(0, null));
    }

    @Test
    void pullTest() throws IOException {
        addData(0, "msg");
        addData(0, "msg1");
        assertEquals(2, topic.count(0, null)); // 전체 메시지 개수 확인

        TopicRecord record = topic.pull(0, "user1", 1);
        try (
            OutputStream out = new ByteArrayOutputStream();
            WritableByteChannel channel = Channels.newChannel(out);
        ) {
            ((FileRegion) record.value()).transferTo(channel, 0);
            assertEquals("msg1", out.toString());
        }

        /*
         * offset이 현재 최대 1인데, (0, 1) 4를 요청하면, null 반환하고, record를 찾지 못했으므로
         * offset 반영도 되지 않음
         */
        topic.pull(0, "user2", 4);
        assertEquals(0, topic.offset(0, "user2"));

        assertEquals(2, topic.offset(0, "user1"));
        assertEquals(0, topic.offset(0, "user2")); // unknown user = 0
        assertEquals(0, topic.offset(1, null)); // unknown partition = 0
    }

    @Test
    void reloadTest() throws IOException {
        addData(0, "msg1");
        addData(0, "msg2");
        topic.segmentManager(0).rollover(System.currentTimeMillis());

        assertEquals(2, topic.count(0, null));
        assertEquals(3, topic.segmentCount(0));

        topic = new DiskTopic(topicName, 0, 3000); // 재생성

        /*
         * 재생성 이후, loadSegment()가 호출되어 기존에 저장된 메시지 로드 되어야 함
         */
        assertEquals(2, topic.count(0, null));

        addData(0, "msg3");

        assertEquals(3, topic.count(0, null));
        assertEquals(3, topic.segmentCount(0));
    }
}
