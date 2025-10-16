package org.example.broker;

import java.util.HashMap;
import java.util.Map;

import org.example.netty.server.NettyServer;
import org.example.topic.Topic;
import org.example.topic.TopicManager;

public class Broker implements AutoCloseable {

    private final NettyServer server;
    private final TopicManager topicManager;

    private Broker(Builder builder) {
        topicManager = new TopicManager(builder.topicInfo);
        server = new NettyServer(builder.port, builder.maxFrameLength, topicManager);
    }

    public void start() throws InterruptedException {  server.start(); }
    public TopicManager getTopicManager() { return topicManager; }
    public Topic getTopic(String name) { return topicManager.getTopic(name); }
    public long getTopicLength(String name) { return getTopic(name).getLength(); }
    public static Builder builder() { return new Builder(); }
    
    @Override 
    public void close() throws Exception { server.close(); }
    
    // inner class
    public static class Builder {
        private int port = 1234;
        private int maxFrameLength = 1024 * 1024; // 1MB(MiB)
        private final Map<String, Topic.Type> topicInfo = new HashMap<>();

        private Builder() {} // 직접 생성 제한

        public Broker build() { return new Broker(this); }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder maxFrameLength(int maxFrameLength) {
            this.maxFrameLength = maxFrameLength;
            return this;
        }

        public Builder addTopic(String name, Topic.Type type) {
            topicInfo.put(name, type);
            return this;
        }

        public Builder addTopics(Map<String, Topic.Type> topics) {
            topics.putAll(topics);
            return this;
        }
    }
}
