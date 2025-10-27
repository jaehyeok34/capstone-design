package capstone.design;

import capstone.design.broker.Broker;
import capstone.design.topic.Topic;

public class App {

    public static void main(String[] args) {
        Broker.Builder builder  = Broker.builder()
            .port(3401)
            .addTopic("test_topic", Topic.Type.DISK);

        try (Broker broker = builder.build()) {
            broker.start();
        } catch (Exception ignored) {}
    }
}
