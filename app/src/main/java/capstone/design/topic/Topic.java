package capstone.design.topic;

import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import capstone.design.message.Message;

public interface Topic {
    String name();
    int push(String partition, Message message);
    @Nullable TopicRecord peek(String partition, String clientId, Message message);
    void commit(String partition, String clientId, int offset, Message message);
    int find(String partition, Map<String, String> condition, Message message);
    boolean seek(String partition, String clientId, int offset, Message message);
    int subscribe(String partition, Supplier<Boolean> callback);
    void unsubscribe(String partition, int key);
    void notify(String partition);
    int count(String partition, Message message);
    void clean();

    // @Nullable TopicRecord peek(Message message);
    // void commit(Message message);
    // boolean seek(Message message);
    // int find(Message message);
    // int subscribe(Message message, Supplier<Boolean> callback);
    // void unsubscribe(Message message, int key);
    // int count(Message message);
    // void clean();

    public enum Type { MEMORY, DISK }
}
