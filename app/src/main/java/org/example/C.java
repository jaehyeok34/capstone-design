package org.example;

import java.nio.charset.StandardCharsets;

import org.example.client.Consumer;
import org.example.message.Message;

public class C {
    
    public static void main(String[] args) throws Exception {
        try (Consumer c = new Consumer(1234)) {
            for (int i = 0; i < 3; i++) {
                Message m = c.request("topic_3");
                if (m == null) {
                    System.out.println("[debug]: message is null");
                    continue;
                }
                
                System.out.println("[debug]: topic name: " + m.getTopicName());
                System.out.println("[debug]: payload: " + m.getPayload().toString(StandardCharsets.UTF_8));

            }
        }
    }
}
