// package capstone.design;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNull;

// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.io.OutputStream;
// import java.nio.channels.Channels;
// import java.nio.channels.WritableByteChannel;
// import java.nio.charset.StandardCharsets;

// import capstone.design.topic.TopicRecord;
// import capstone.design.topic.disk.DiskTopic;

// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// import io.netty.channel.FileRegion;

// public class DiskTopicTest {

//     final String topicName = "test_topic";
//     DiskTopic topic;

//     void addData(int partition, String msg) {
//         topic.push(partition, null, msg.getBytes(StandardCharsets.UTF_8));
//     }

//     @BeforeEach
//     void beforeEach() throws IOException {
//         topic = DiskTopic.of(topicName, 0, 1000);
//     }

//     @AfterEach
//     void afterEach() throws IOException {
//         topic.clearAll();
//     }

//     @Test
//     void pushTest() throws InterruptedException {
//         addData(0, "msg1");
//         addData(0, "msg2");
//         topic.segmentManager(0).rollover(System.currentTimeMillis());

//         /*
//          * segment duration을 0ms로 설정 즉, 매번 새로운 세그먼트 생성하기 때문에
//          * 2개의 메시지를 저장했으므로 세그먼트는 2개지만
//          * 명시적으로 rollover()를 호출했기 때문에 총 3개가 됨
//          */
//         assertEquals(3, topic.segmentCount(0));
//         assertEquals(2, topic.count(0, null));
//     }

//     @Test
//     void pullTest() throws IOException {
//         addData(0, "msg1");
//         addData(0, "msg2");
//         assertEquals(2, topic.count(0, null)); // 전체 메시지 개수 확인

//         TopicRecord record = topic.pull(0, "user1", 1L);
//         assertEquals("msg2", readFileRegion((FileRegion) record.value()));

//         /*
//          * offset이 현재 최대 1인데, (0, 1) 4를 요청하면, null 반환하고, record를 찾지 못했으므로
//          * offset 반영도 되지 않음
//          */
//         Object o = topic.pull(0, "user2", 4L);
//         assertNull(o);
//         assertEquals(-1, topic.offset(0, "user2"));

//         assertEquals(2, topic.offset(0, "user1"));
//         assertEquals(-1, topic.offset(0, "user2")); // unknown user = 0
//         assertEquals(-1, topic.offset(1, null)); // unknown partition = 0
//     }

//     @Test
//     void reloadTest() throws IOException {
//         addData(0, "msg1");
//         addData(0, "msg2");
//         topic.segmentManager(0).rollover(System.currentTimeMillis());

//         assertEquals(2, topic.count(0, null));
//         assertEquals(3, topic.segmentCount(0));

//         topic = DiskTopic.of(topicName, 0, 3000); // 재생성

//         /*
//          * 재생성 이후, loadSegment()가 호출되어 기존에 저장된 메시지 로드 되어야 함
//          */
//         assertEquals(2, topic.count(0, null));

//         addData(0, "msg3");

//         assertEquals(3, topic.count(0, null));
//         assertEquals(3, topic.segmentCount(0));
//     }

//     @Test
//     void cleanTest() throws Exception {
//         addData(0, "msg1");
//         addData(0, "msg2");
//         addData(0, "msg3");

//         assertEquals(3, topic.count(0, null));
//         assertEquals(3, topic.segmentCount(0));

//         TopicRecord response = topic.pull(0, "user");
//         assertEquals("msg1", readFileRegion((FileRegion) response.value()));

//         Thread.sleep(1100);
//         topic.clean();

//         /*
//          * 첫 번째 메시지를 읽었으므로 offset은 1이 되어야 하지만,
//          * clean()을 통해 msg1, msg2에 대한 segment 파일이 삭제되면서
//          * 남아있는 메시지(current segment, msg3)의 base offset인 2보다 작으므로
//          * "user"의 offset 정보가 삭제됨
//          */
//         assertEquals(-1, topic.segmentManager(0).offset("user"));

//         /*
//          * offset을 지정하지 않고 pull()을 호출하게 되면,
//          * 1. offset에 -1로 지정되게 되고,
//          * 2. segment.read()에서 offsets 파일에서 client id에 해당하는 offset을 읽는데
//          * 3. 위에서 삭제됐으므로 가장 오래된 세그먼트(current segment)의 base offset인 2가 사용되어
//          * 4. 결국 msg3이 반환됨
//          */
//         response = topic.pull(0, "user");
//         assertEquals("msg3", readFileRegion((FileRegion) response.value()));

//         /*
//          * clean() 이후 프로그램이 재시작 됐다 가정하고 topic을 재생성 하면
//          * 남아있는 segment(current segment)가 로드되어 메모리에 올라가는지 확인
//          * rollover()를 호출하는 이유는, current segment가 아직 .meta에 기록되지 않았기 때문에
//          * 강제 기록하기 위함
//          */
//         topic.segmentManager(0).rollover(System.currentTimeMillis());
//         topic = DiskTopic.of(topicName, 0, 3000);
//         assertEquals(1, topic.segmentCount(0));
//         assertEquals(1, topic.segmentManager(0).messageCount());
//     }

//     String readFileRegion(FileRegion region) throws IOException {
//         try (
//             OutputStream out = new ByteArrayOutputStream();
//             WritableByteChannel channel = Channels.newChannel(out);
//         ) {
//             region.transferTo(channel, 0);
//             return out.toString();
//         }
//     }
// }
