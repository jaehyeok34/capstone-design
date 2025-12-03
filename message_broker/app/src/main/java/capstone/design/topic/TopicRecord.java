package capstone.design.topic;

import java.util.Map;

import capstone.design.message.Message;


public class TopicRecord {

    private final Message message;
    private final long createdAt;

    public TopicRecord(Message message) {
        this.message = message;
        this.createdAt = System.currentTimeMillis();
    }

    public Message message() { return message; }
    public boolean isExpired(long retention) { return (System.currentTimeMillis() - createdAt) > retention; }

    public boolean matches(Map<String, String> condition) {
        Map<String, String> header = message.header();
        for (Map.Entry<String, String> entry : condition.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            // 모든 조건에 대하여, 해당하는 키가 존재하지 않거나, 값이 다른 경우가 하나라도 존재하면 false 반환
            if (!header.containsKey(key) || !header.get(key).equals(value)) {
                return false;
            }
        }
        
        return true;
    }

    @Override
    public String toString() {
        return "{message=" + message + ", createdAt=" + createdAt + "}";
    }
}