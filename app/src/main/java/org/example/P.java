package org.example;

import org.example.client.Producer;

public class P {
    
    public static void main(String[] args) throws Exception {
        try (Producer producer = new Producer(1234)) {
            int counter = 0;
            for (int i = 0; i < 5; i++) {
                String input = "message" + (counter++);
                producer.request("t2", input);
            }
        }
    }
}