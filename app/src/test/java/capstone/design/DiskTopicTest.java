package capstone.design;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import capstone.design.topic.disk.DiskTopic;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DiskTopicTest {

    final String topicName = "test_topic";
    DiskTopic topic;

    void addData(int partition, String msg) {
        ByteBuf buf = Unpooled.copiedBuffer((msg).getBytes(StandardCharsets.UTF_8));
        topic.push(partition, null, buf);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        topic = new DiskTopic(topicName, 0);
    }

    @AfterAll
    void afterAll() throws IOException {
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
        assertEquals(2, topic.messageCount(0));
    }


    //     TopicRecord record = topic.pull(0) instanceof TopicRecord r ? r : null;
    //     if (record.value() instanceof FileRegion region) {
    //         try (
    //             OutputStream out = new ByteArrayOutputStream();
    //             WritableByteChannel channel = Channels.newChannel(out);
    //         ) {
    //             region.transferTo(channel, 0);
    //             assertEquals(message + 1, out.toString());
    //             assertEquals(2, topic.cursor(0));
    //         } catch (IOException ignore) {}
    //     }
    // }
    
    // @Test
    // void subscribeTest() {
    //     SpyContext context = new SpyContext();
    //     DiskTopic topic = topics.getFirst();
    //     int partition = 0;
        
    //     assertEquals(0, topic.subscriberManager().count(partition));

    //     topic.subscribe(context, partition, "s1");
    //     assertEquals(1, topic.subscriberManager().count(partition));

    //     assertDoesNotThrow(() -> topic.unsubscribe(partition, "s0"));

    //     topic.unsubscribe(partition, "s1");
    //     assertEquals(0, topic.subscriberManager().count(partition));
    // }
}
