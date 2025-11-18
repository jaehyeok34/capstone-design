package capstone.design.topic;

import java.util.Collection;
import org.jspecify.annotations.Nullable;

import capstone.design.message.Message;

public interface Topic {
    String name();

    /**
     * 메시지 저장
     * @param message {@code partition, payload} 필수
     * @return 저장 성공 시 {@code offset}, 실패 시 {@code -1} 반환
     */
    int push(Message message);
    
    @Nullable TopicRecord pull(Message message);
    boolean seek(Message message);
    int find(Message message);
    int subscribe(Message message, Collection<Object> out);
    void unsubscribe(Message message, int key);
    int count(Message message);
    void clean();

    public enum Type { MEMORY, DISK }
}
