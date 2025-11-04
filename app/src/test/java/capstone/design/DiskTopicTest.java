package capstone.design;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import capstone.design.topic.disk.DiskTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DiskTopicTest {

    final String topicName = "test_topic";
    final List<ByteBuf> bufs = new ArrayList<>();
    DiskTopic topic;

    void addData(int partition, String msg) {
        ByteBuf buf = Unpooled.copiedBuffer((msg).getBytes(StandardCharsets.UTF_8));
        bufs.add(buf);
        topic.push(partition, null, buf);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        topic = new DiskTopic(topicName, 0);
    }

    @AfterEach
    void afterEach() throws IOException {
        topic.clearAll();

        // topics = null;
        bufs.forEach(buf -> { if(buf.refCnt() > 0) buf.release(); });
    }

    @Test
    void pushTest() throws InterruptedException {
        addData(0, "msg");
        Thread.sleep(1000);
        addData(0, "msg");
        topic.segmentManager(0).rollover(System.currentTimeMillis());

        /*
         * segment duration을 0ms로 설정 즉, 매번 새로운 세그먼트 생성하기 때문에
         * 2개의 메시지를 저장했으므로 세그먼트는 2개지만
         * 명시적으로 rollover()를 호출했기 때문에 총 3개가 됨
         */
        assertEquals(3, topic.segmentCount(0));
        assertEquals(2, topic.messageCount(0));
    }


    // @Test
    // void pullTest() throws Exception {
    //     // 데이터 준비
    //     DiskTopic topic = topics.getFirst();
        
    //     for (int i = 0; i < 3; i++) {
    //         addData(topic, 0, String.valueOf(i));
    //     }

    //     // pull
    //     for (int i = 0; i < 2; i++) { // 동일 조건으로 동일 커서 2회 검증
    //         TopicRecord record = topic.pull(0, 0) instanceof TopicRecord r ? r : null;
    //         if (record.value() instanceof FileRegion region) {
    //             try (
    //                 OutputStream out = new ByteArrayOutputStream();
    //                 WritableByteChannel channel = Channels.newChannel(out);
    //             ) {
    //                 region.transferTo(channel, 0);
    
    //                 // cursor: 0을 읽었을 때 검증
    //                 assertEquals(message + 0, out.toString());
    //                 assertEquals(1, topic.cursor(0));
    //             } catch (IOException ignore) {}
    //         }
    //     }

    //     // 다음 cursor 읽기 검증
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
