package capstone.design;

import capstone.design.broker.Broker;
import capstone.design.topic.disk.DiskTopic;
import capstone.design.topic.memory.MemoryTopic;

public class App {
    public static void main(String[] args) throws Exception {
        long duration = 1 * 60 * 1000; // segment rollover 기간: 1분
        long retention = 3 * 60 * 1000; // 메시지(혹은 세그먼트) 보관 기간: 3분
        String convert = "convert";
        String join = "join";
        Broker broker  = Broker.builder()
            .port(3401)
            .addTopic(join, DiskTopic.of(join, duration, retention))
            .addTopic(convert, MemoryTopic.of(convert, retention)) // 1분 동안 메모리에 보관
            .cleanInterval(10 * 1000) // 10초마다 메시지 클리너 실행
            .build();

        broker.start();
    }
}
