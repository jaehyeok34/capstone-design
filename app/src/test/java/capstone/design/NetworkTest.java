// package capstone.design;

// import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.io.IOException;
// import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;

// import capstone.design.broker.Broker;
// import capstone.design.client.Consumer;
// import capstone.design.client.Producer;
// import capstone.design.message.Message;
// import capstone.design.topic.Topic;
// import capstone.design.topic.disk.DiskTopic;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;

// public class NetworkTest {

//     Broker broker;
//     ExecutorService executor;

//     final String t1 = "memory_topic";
//     final String t2 = "disk_topic";
//     final int port = 3400;
//     final byte[] msg = "hi".getBytes(StandardCharsets.UTF_8);

//     @BeforeEach
//     void beforeEach() throws Exception {
//         broker = Broker.builder()
//             .port(port)
//             .addTopic(t1, Topic.Type.MEMORY)
//             .addTopic(t2, DiskTopic.of(t2, 0, 1000))
//             .build();

//         executor = Executors.newSingleThreadExecutor();
//         executor.submit(() -> {
//             try {
//                 broker.start();
//             } catch (Exception ignored) {}

//             broker.close();
//         });

//         while (!broker.isActive()) {
//             Thread.sleep(100);
//         } // 서버가 열릴 때까지 대기
//     }

//     @AfterEach
//     void afterEach() throws IOException {
//         broker.close();
//         if (broker.topic(t2) instanceof DiskTopic topic) {
//             topic.clearAll();
//         }
//         executor.shutdownNow();
//     }

//     @Test
//     void invalidTopicProduceTest() throws Exception {
//         try (Producer producer = new Producer("localhost", port, "user")) {
//             assertDoesNotThrow(() -> {
//                 /*
//                  * 존재하지 않는 토픽에 메시지 전송
//                  * 응답 메시지에 ok=0 포함됨
//                  */
//                 Message response = producer.syncProduce("invalid_topic", 0, msg, 9999, TimeUnit.SECONDS);
//                 assertTrue(response.optionAsByte("ok").equals((byte) 0));

//                 /*
//                  * 토픽은 있지만, 파티션이 없는 경우에는
//                  * 내부적으로 파티션을 생성하여 추가하기 때문에 ok=1
//                  * 또한, 값이 정상적으로 추가 됐는지 확인
//                  */
//                 response = producer.syncProduce(t1, 99, msg, 9999, TimeUnit.SECONDS);
//                 assertTrue(response.optionAsByte("ok").equals((byte) 1));
//                 assertEquals(1, broker.topic(t1).count(99, "user"));
//             });
//         }
//     }

//     @Test
//     void produceTest() throws Exception {
//         String clientId = "user";
//         try (Producer producer = new Producer("localhost", port, clientId)) {
//             // 메시지 각각 2번씩 전송
//             for (int i = 0; i < 2; i++) {
//                 producer.asyncProduce(t1, 0, clientId);
//                 producer.asyncProduce(t2, 0, clientId);
//             }

//             // 브로커가 메시지를 처리(저장)할 시간 대기
//             Thread.sleep(2000);

//             /*
//              * 값이 2개 잘 들어갔는지 확인
//              * 단, 메모리 토픽의 경우 같은 파티션이라고 하더라도 client id가 별로
//              * 개별적인 자료구조에 메시지를 저장하기 때문에, unknown_user는 0이어야 함
//              */
//             assertEquals(2, broker.topic(t1).count(0, clientId));
//             assertEquals(0, broker.topic(t1).count(0, "unknown_user"));

//             /*
//              * 디스크 토픽의 경우 파티션 단위로 메시지를 저장하기 때문에
//              * client id와 상관없이 동일 파티션에 대해서는 항상 같은 count를 반환해야 함
//              */
//             assertEquals(2, broker.topic(t2).count(0, clientId));
//             assertEquals(2, broker.topic(t2).count(0, "unknown_user"));
//         }
//     }

//     @Test
//     void invalidTopicConsumeTest() throws Exception {
//         try (Consumer consumer = new Consumer("localhost", port, "user")) {
//             Message response = consumer.consume(t1, 0, 5, TimeUnit.SECONDS);
//             assertTrue(response.optionAsByte("ok").equals((byte) 0));
//             assertEquals(null, response.option("payload"));

//             response = consumer.consume(t2, 0, 5, TimeUnit.SECONDS);
//             assertTrue(response.optionAsByte("ok").equals((byte) 0));
//             assertEquals(null, response.option("payload"));
//         }
//     }

//     @Test
//     void consumeTest() throws Exception {
//         addData();
//         try (Consumer consumer = new Consumer("localhost", port, "user")) {
//             Message response = consumer.consume(t1, 0, 3, 1, TimeUnit.SECONDS); // 잘못된 offset
//             assertTrue(response.optionAsByte("ok").equals((byte) 0));
//             assertEquals(null, response.option("payload"));

//             response = consumer.consume(t1, 0, 5, TimeUnit.SECONDS); // offset 생략 == FIFO
//             assertTrue(response.optionAsByte("ok").equals((byte) 1));
//             assertEquals(new String(msg, StandardCharsets.UTF_8), response.optionAsString("payload"));
//         }
//     }

//     @Test
//     void subscribeTest() throws Exception {
//         try (
//             Consumer consumer1 = new Consumer("localhost", port, "c1");
//             Consumer consumer2 = new Consumer("localhost", port, "c2");
//             Producer producer = new Producer("localhost", port, "p1")
//         ) {
//             List<Message> notified = new ArrayList<>();
//             ExecutorService notifier1 = consumer1.subscribe(t1, 0, notified, 9999, TimeUnit.SECONDS);
//             ExecutorService notifier2 = consumer2.subscribe(t1, 0, notified, 9999, TimeUnit.SECONDS);

//             // producer가 토픽에 데이터 추가
//             Message response = producer.syncProduce(t1, 0, "msg".getBytes(StandardCharsets.UTF_8), 9999, TimeUnit.SECONDS);
//             assertTrue(response.optionAsByte("ok").equals((byte) 1));

//             Thread.sleep(1000); // 알림 받을 시간 대기

//             // 구독자 2명 모두 알림을 받기 때문에 2개여야 함
//             assertEquals(2, notified.size());

//             // 구독자 1명 해제
//             notifier2.shutdownNow();

//             // 다시 데이터 추가
//             response = producer.syncProduce(t1, 0, "msg".getBytes(StandardCharsets.UTF_8), 9999, TimeUnit.SECONDS);
//             assertTrue(response.optionAsByte("ok").equals((byte) 1));

//             Thread.sleep(1000); // 알림 받을 시간 대기

//             assertEquals(3, notified.size()); // 구독자 1명만 알림 받아야 함

//             notifier1.shutdownNow();
//         }
//     }

//     void addData() throws Exception {
//         for (String t : List.of(t1, t2)) {
//             broker.topic(t).push(0, "user", msg);
//         }
//     }
// }
