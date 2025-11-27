package capstone.design;

import capstone.design.broker.Broker;
import capstone.design.topic.memory.MemoryTopic;

public class App {

    public static void main(String[] args) throws Exception {
        long retention = 1 * 60 * 1000; // 1분
        String convert = "convert";
        String join = "join";
        Broker broker  = Broker.builder()
            .port(3401)
            .addTopic(convert, MemoryTopic.of(convert, retention)) // 1분 동안 메모리에 보관
            .addTopic(join, MemoryTopic.of(join, retention))
            .cleanInterval(10 * 1000) // 10초마다 메시지 클리너 실행
            .build();

        broker.start();
    }
}
