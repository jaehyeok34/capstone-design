// package capstone.design;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertNull;

// import java.nio.charset.StandardCharsets;
// import capstone.design.topic.memory.MemoryTopic;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// public class MemoryTopicTest {

//     private MemoryTopic topic;
//     private final int partition = 0;
//     private final String clientId = "user";
//     private final String message = "message";

//     void addData() {
//         for (int i = 0; i < 2; i++) {
//             topic.push(partition, clientId + i, (message + i).getBytes(StandardCharsets.UTF_8));
//         }
//     }

//     @BeforeEach
//     void beforeEach() {
//         topic = MemoryTopic.of("test_topic", 3000); // 3초 유지
//     }

//     @Test
//     void pushTest() {
//         addData();
        
//         /*
//          * 첫 번째 id는 2개의 메시지가 쌓여야함
//          * partition에 대하여 push하게 되면, partition에 있는 id 전체에게 메시지가 쌓이기 때문에
//          * 첫 번째 id는 2회 push(자신 + 다음), 두 번째 id는 1회 push(자신)이기 때문
//          */
//         assertEquals(2, topic.count(partition, clientId + 0));
//         assertEquals(1, topic.count(partition, clientId + 1));

//         // 없는 partition에 대해서 length 확인
//         assertEquals(0, topic.count(partition + 1, clientId + 0));

//         // 없는 id에 대해서 length 확인
//         assertEquals(0, topic.count(partition, clientId + 2));
//     }

//     @Test
//     void pullTest() {
//         addData();

//         Object pulled0 =  topic.pull(partition, clientId + 0, 3L);
//         assertNull(pulled0);

//         String pulled1 = new String((byte[]) topic.pull(partition, clientId + 0, 1L).value(), StandardCharsets.UTF_8);
        
//         // offset을 1로 지정했기 때문에 message1이 나와야 함
//         assertEquals(message + 1, pulled1);

//         String pulled2 = new String((byte[]) topic.pull(partition, clientId + 1).value(), StandardCharsets.UTF_8);
        
//         // offset을 지정하지 않았기 때문에 message0이 나와야 함(FIFO)
//         assertEquals(message + 1, pulled2);

//         // clientId + 0은 원래 2개의 메시지가 있었고, 하나를 뻇으니까 1개 남아야 함
//         assertEquals(1, topic.count(partition, clientId + 0));
//     }

//     @Test
//     void cleanTest() throws Exception {
//         topic.push(0, "user1", "msg1".getBytes(StandardCharsets.UTF_8));
//         topic.push(1, "user1", "msg2".getBytes(StandardCharsets.UTF_8));

//         Thread.sleep(1000); // 1초 대기
//         topic.push(0, "user2", "msg3".getBytes(StandardCharsets.UTF_8));
//         topic.push(1, "user2", "msg4".getBytes(StandardCharsets.UTF_8));

//         Thread.sleep(1000); // 1초 대기
//         topic.push(0, "user1", "msg5".getBytes(StandardCharsets.UTF_8));
//         topic.push(1, "user1", "msg6".getBytes(StandardCharsets.UTF_8));

//         /*
//          * user1은 2개고 user2는 1개씩 쌓이는 이유는
//          * user1이 먼저 push되면서 partition에 user1에 대한 저장소가 생성되어 있으므로
//          * user2가 push 하더라도 user1에게도 메시지가 쌓이기 때문
//          */
//         assertEquals(3, topic.count(0, "user1"));
//         assertEquals(3, topic.count(1, "user1"));

//         assertEquals(2, topic.count(0, "user2"));
//         assertEquals(2, topic.count(1, "user2"));

//         Thread.sleep(2000); // 2초 대기
        
//         /*
//          * msg1, 2는 생성된지 4초가 경과하여 삭제되어야 함
//          * msg3, 4 역시 생성된지 3초가 경과하여 삭제되어야 함
//          */
//         topic.clean();

//         /*
//          * msg5(0), msg6(1)만 남아있기 때문에 각각 1개씩 남아야 함
//          * 1. 만료된 메시지가 잘 삭제 됐는지(msg1 ~ 4)
//          * 2. 정상적인 메시지는 삭제되지 않는지(msg5 ~ 6)
//          * 3. 모든 파티션, 저장소에 대해 동작하는지(user2역시 원래 2였는데 1로 줄어들었는지)
//          */
//         assertEquals(1, topic.count(0, "user1"));
//         assertEquals(1, topic.count(1, "user1"));
//         assertEquals(1, topic.count(0, "user2"));
//         assertEquals(1, topic.count(1, "user2"));
//     }
// }
