package capstone.design;

import capstone.design.broker.Broker;
import capstone.design.topic.Topic;
import capstone.design.topic.disk.DiskTopic;

public class App {

    public static void main(String[] args) throws Exception {
        Broker broker  = Broker.builder()
            .port(3401)
            .addTopic("test_topic", Topic.Type.DISK)
            .addTopic("convert_file", DiskTopic.of("convert_file", 30 * 1000, 3 * 60 * 1000))
            .build();

        broker.start();
    }
}
