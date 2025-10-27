package org.example.broker;

import java.util.HashMap;
import java.util.Map;
import org.example.netty.server.NettyServer;
import org.example.topic.Topic;
import org.example.topic.TopicManager;

public class Broker implements AutoCloseable {

    private final TopicManager topicManager;
    private final NettyServer server;

    private Broker(Builder builder) {
        topicManager = new TopicManager(builder.topicInfo);
        server = new NettyServer(builder.port, topicManager);
    }
    
    public static Builder builder() { return new Builder(); }

    public void start() throws InterruptedException {  server.start(); }
    public TopicManager topicManager() { return topicManager; }
    public Topic topic(String name) { return topicManager.topic(name); }
    public boolean isActive() { return server.isActive(); }
    
    @Override 
    public void close() {
        server.shutdown();
    }
    
    // inner class
    public static class Builder {
        private int port = 1234;
        private final Map<String, Topic.Type> topicInfo = new HashMap<>();

        private Builder() {} // 직접 생성 제한

        public Broker build() { 
            if (topicInfo.isEmpty()) {
                throw new IllegalStateException("topicInfo: empty");
            }

            return new Broker(this); 
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder addTopic(String name, Topic.Type type) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name: null or empty");
            }

            if (type == null) {
                throw new IllegalArgumentException("type: null");
            }

            topicInfo.put(name, type);
            return this;
        }

        public Builder addTopics(Map<String, Topic.Type> topics) {
            // 유효한 항목만 추가
            topics.forEach((key, value) -> {
                try {
                    addTopic(key, value);
                } catch (IllegalArgumentException ignore) {
                    System.err.println("[debug] 토픽 추가 거부: " + key + ", " + value);
                }
            });

            return this;
        }
    }
}
