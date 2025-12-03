package capstone.design.broker;

import java.util.HashMap;
import java.util.Map;

import capstone.design.Utils;
import capstone.design.netty.server.NettyServer;
import capstone.design.topic.Topic;
import capstone.design.topic.TopicManager;
import capstone.design.topic.disk.DiskTopic;
import capstone.design.topic.memory.MemoryTopic;

public class Broker implements AutoCloseable {

    private final TopicManager topicManager;
    private final NettyServer server;

    // constructor =====
    private Broker(Builder builder) throws Exception {
        topicManager = TopicManager.of(builder.topics, builder.cleanInterval);
        server = new NettyServer(builder.port, topicManager);
    }
    
    // factory method ===== 
    public static Builder builder() { return new Builder(); }

    // getter =====
    public TopicManager topicManager() { return topicManager; }
    public Topic topic(String name) { return topicManager.topic(name); }
    public boolean isActive() { return server.isActive(); }

    // method =====
    public void start() throws InterruptedException { 
        System.out.println("! broker start");
        server.start(); 
    }
    
    @Override 
    public void close() {
        server.shutdown();
        topicManager.shutdownNow();
    }
    
    // inner class
    public static class Builder {
        private int port = 1234;
        private final Map<String, Topic> topics = new HashMap<>();
        private long cleanInterval = -1;

        private Builder() {} // 직접 생성 제한

        public Broker build() throws Exception { 
            Utils.validate(topics);

            return new Broker(this); 
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder addTopic(String name, Topic.Type type) {
            if (Utils.isValid(name, type)) {
                try {
                    topics.put(name, switch (type) {
                        case MEMORY -> MemoryTopic.of(name);
                        case DISK -> DiskTopic.of(name);
                    });
                } catch (Exception ignored) {}
            }

            return this;
        }

        public Builder addTopic(String name, Topic topic) {
            if (Utils.isValid(name, topic)) {
                topics.put(name, topic);
            }

            return this;
        }

        public Builder cleanInterval(long cleanInterval) {
            this.cleanInterval = cleanInterval;
            return this;
        }
    }
}
