package org.example;

import org.example.broker.Broker;
import org.example.topic.Topic;

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
