package capstone.design;

import capstone.design.broker.Broker;
import capstone.design.topic.Topic;
import capstone.design.topic.memory.MemoryTopic;

public class App {

    public static void main(String[] args) throws Exception {
        Broker broker  = Broker.builder()
            .port(3401)
            .addTopic("test_topic", Topic.Type.DISK)
            .addTopic("convert_file", MemoryTopic.of(30 * 60 * 1000))
            // .addTopic("convert_file", DiskTopic.of("convert_file", 0, 30 * 1000))
            .cleanInterval(10 * 1000) // 10초마다 메시지 클리너 실행
            .build();

        broker.start();
    }
}
