package capstone.design.message;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

public class Message {
    private MessageType type; // non-null. builder에서 반드시 설정하도록 설계함
    private final Map<String, String> header = new HashMap<>();
    private @Nullable Object payload;

    // constructor =========================================
    private Message(MessageType type, Map<String, String> header, @Nullable Object payload) {
        this.type = type;
        this.header.putAll(header);
        this.payload = payload;
    }

    // static methods =========================================
    public static Builder builder() { return new Builder(); }

    // getters =========================================
    public MessageType type() { return type; }
    public Map<String, String> header() { return header; }
    public @Nullable String header(String key) { return header.get(key); }
    public String header(String key, String defaultValue) { return header.getOrDefault(key, defaultValue); }
    public int header(String key, int defaultValue) { return parseHeader(key, defaultValue, Integer::parseInt); }
    public long header(String key, long defaultValue) { return parseHeader(key, defaultValue, Long::parseLong); }
    public @Nullable Object payload() { return payload; }

    public String topicName() { return header("topic.name", ""); }
    public String partition() { return header("partition", ""); }
    public String clientId() { return header("client.id", ""); }
    public int offset() { return header("offset", -1); }
    public long timeout() { return header("timeout", 0L); }
    public int count() { return header("count", 1); }

    public Map<String, String> condition() {
        Map<String, String> condition = new HashMap<>();
        for (Map.Entry<String, String> header : header.entrySet()) {
            String key = header.getKey();
            if (key.startsWith("condition.")) {
                condition.put(key.substring("condition.".length()), header.getValue());
            }
        }

        return condition;
    }

    // methods =========================================
    public Message setType(MessageType type) {
        this.type = type;
        return this;
    }

    public Message addHeader(String key, String value) {
        header.put(key, value);
        return this;
    }

    public Message addHeader(String key, Object value) {
        header.put(key, String.valueOf(value));
        return this;
    }

    public Message addHeader(Map<String, String> header) {
        this.header.putAll(header);
        return this;
    }
    
    public Message removeHeader(String... keys) {
        for (String key : keys) {
            header.remove(key);
        }
        
        return this;
    }
    
    public Message removePayload() {
        this.payload = null;
        return this;
    }

    public Message copy() {
        return new Message(type, header, payload);
    }

    private <T> T parseHeader(String key, T defaultValue, Function<String, T> parser) {
        String value = header.get(key);
        try {
            return parser.apply(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // override =========================================
    @Override
    public String toString() {
        String str = "";
        str += "type: " + type.name() + ", ";
        for (Map.Entry<String, String> entry : header.entrySet()) {
            str += entry.getKey() + ": " + entry.getValue() + ", ";
        }
        str += "payload: " + (payload != null ? "O" : "X");

        return "Message{" + str + "}";
    }

    // inner class =========================================
    public static class Builder {
        private MessageType type = null;
        private final Map<String, String> header = new HashMap<>();
        private Object payload = null;

        private Builder() {}

        public Message build() {
            if (type == null) {
                throw new IllegalStateException("! Message.Builder.build(): 메시지 타입 미설정");
            }

            return new Message(type, header, payload);
        }

        public Builder type(MessageType type) {
            this.type = type;
            return this;
        }

        public Builder type(byte type) {
            this.type = MessageType.values()[type];
            return this;
        }

        public Builder header(String key, String value) {
            header.put(key, value);
            return this;
        }

        public Builder header(Map<String, String> header) {
            this.header.putAll(header);
            return this;
        }

        public Builder condition(String key, String value) {
            header.put("condition." + key, value);
            return this;
        }

        public Builder condition(Map<String, String> condition) {
            for (Map.Entry<String, String> entry : condition.entrySet()) {
                condition(entry.getKey(), entry.getValue());
            }

            return this;
        }

        public Builder topicName(String topicName) {
            header.put("topic.name", topicName);
            return this;
        }

        public Builder partition(int partition) {
            header.put("partition", String.valueOf(partition));
            return this;
        }

        public Builder partition(String partition) {
            header.put("partition", partition);
            return this;
        }

        public Builder clientId(String clientId) {
            header.put("client.id", clientId);
            return this;
        }

        public Builder timeout(long timeout) {
            header.put("timeout", String.valueOf(timeout));
            return this;
        }

        public Builder count(int count) {
            header.put("count", String.valueOf(count));
            return this;
        }

        public Builder offset(int offset) {
            header.put("offset", String.valueOf(offset));
            return this;
        }

        public Builder payload(Object payload) {
            this.payload = payload;
            return this;
        }

        public Builder error(String msg) {
            header.put("error", msg);
            return this;
        }
    }
}
