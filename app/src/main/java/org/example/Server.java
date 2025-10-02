package org.example;

import org.example.broker.Broker;
import org.example.broker.Broker.Builder;
import org.example.broker.topic.Topic;

public class Server {

    public static void main(String[] args) throws InterruptedException {
        Builder builder = new Broker.Builder()
            .port(1234)
            .addTopic("t1", Topic.Type.MEMORY);

        try (Broker broker = builder.build()) {
            System.out.println("[debug]: 브로커 시작");
            broker.start();
        } catch (Exception e) { e.printStackTrace(); }
    }
}
