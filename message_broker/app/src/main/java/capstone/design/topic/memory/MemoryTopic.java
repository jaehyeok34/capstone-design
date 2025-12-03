package capstone.design.topic.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import capstone.design.topic.TopicRecord;
import capstone.design.topic.subscribe.SubscribeManager;
import capstone.design.message.Message;
import capstone.design.topic.Topic;

public class MemoryTopic implements Topic {
    
    private static final long DEFAULT_RETENTION = 3 * (60 * 1000); // 3분

    private final Map<String, Map<Integer, TopicRecord>> storages = new ConcurrentHashMap<>();

    /*
     * offsets: partition의 논리 오프셋 관리(다음 저장 값)
     * clientOffsets: client.id에 따른 오프셋(다음 읽을 값)
     */
    private final Map<String, Integer> offsets = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> clientOffsets = new ConcurrentHashMap<>();
    private final SubscribeManager subscribeManager = new SubscribeManager();
    private final long retention;
    private final String name;

    private MemoryTopic(String name, long retention) {
        this.name = name;
        this.retention = retention;
    }

    public static MemoryTopic of(String name) { return new MemoryTopic(name, DEFAULT_RETENTION); }
    public static MemoryTopic of(String name, long retention) { return new MemoryTopic(name, retention); }

    private void log(String caller) {
        System.out.println(
            "!=== MemoryTopic 상태(" + caller + ") ===" + "\n" +
            " name: " + name + "\n" +
            " storages: " + storages + "\n" +
            " offsets: " + offsets + "\n" +
            " clientOffsets: " + clientOffsets
        );
    }

    @Override public String name() { 
        return name; 
    }

    @Override
    public int push(String partition, Message message) {
        Map<Integer, TopicRecord> storage = storages.computeIfAbsent(partition, ignored -> {
            return new ConcurrentHashMap<>();
        });

        int offset = offsets.compute(partition, (ignored, old) -> {
            return (old == null) ? 1 : old + 1;
        });

        message.addHeader("offset", String.valueOf(offset));
        storage.put(offset, new TopicRecord(message));

        log("push");

        return offset;
    }

    @Override
    public @Nullable TopicRecord peek(String partition, String clientId, Message message) {
        Map<Integer, TopicRecord> storage = storages.get(partition);
        if (storage == null || storage.isEmpty()) {
            System.err.println("? MemoryTopic.peek(): 파티션에 메시지 없음");
            return null;            
        }

        int defaultOffset = Collections.min(storage.keySet());
        int clientOffset = clientOffsets.computeIfAbsent(partition, ignored -> {
            return new ConcurrentHashMap<>();
        }).getOrDefault(clientId, defaultOffset);
        
        clientOffset = Math.max(clientOffset, defaultOffset);

        TopicRecord record = storage.get(clientOffset);
        if (record == null || record.isExpired(retention)) {
            System.err.println("? MemoryTopic.peek(): 유효하지 않은 메시지");
            return null;
        }

        log("peek");

        return record;
    }

    @Override
    public void commit(String partition, String clientId, int offset, Message message) {
        storages.computeIfPresent(partition, (ignored, storage) -> {
            storage.remove(offset);
            return storage;
        });

        clientOffsets.computeIfAbsent(partition, ignored -> {
            return new ConcurrentHashMap<>();
        }).put(clientId, offset + 1);

        log("commit");
    }

    @Override
    public int find(String partition, Map<String, String> condition, Message message) {
        Map<Integer, TopicRecord> storage = storages.get(partition);
        if (storage == null || storage.isEmpty()) {
            System.err.println("? MemoryTopic.find(): 빈 파티션");
            return -1;
        }
        
        // 조건에 맞는 메시지의 오프셋을 모두 획득
        List<Integer> finded = new ArrayList<>();
        for (Map.Entry<Integer, TopicRecord> entry : storage.entrySet()) {
            TopicRecord record = entry.getValue();
            if (record.matches(condition) && !record.isExpired(retention)) {
                finded.add(entry.getKey());
            }
        }
        
        if (finded.isEmpty()) {
            System.err.println("? MemoryTopic.find(): 탐색 실패: " + name + "." + partition);
            return -1;
        }
        
        // 가장 작은 오프셋(FIFO)을 반환
        return Collections.min(finded);
    }

    @Override
    public boolean seek(String partition, String clientId, int offset, Message message) {
        clientOffsets.computeIfAbsent(partition, ignored -> {
            return new ConcurrentHashMap<>();
        }).put(clientId, offset);

        log("seek");
        return true;
    }

    @Override
    public int subscribe(String partition, Supplier<Boolean> callback) {
        return subscribeManager.subscribe(partition, callback);
    }

    @Override
    public void unsubscribe(String partition, int key) {
        subscribeManager.unsubscribe(partition, key);
    }

    @Override
    public void notify(String partition) {
        subscribeManager.notify(partition);
    }

    @Override
    public int count(String partition, Message message) {
        Map<Integer, TopicRecord> storage = storages.get(partition);
        if (storage == null || storage.isEmpty()) {
            return 0;
        }
        
        return storage.size();
    }

    @Override
    public void clean() {
        for (Map<Integer, TopicRecord> storage : storages.values()) {
            Iterator<TopicRecord> iterator = storage.values().iterator();
            while (iterator.hasNext()) {
                TopicRecord record = iterator.next();
                if (record.isExpired(retention)) {
                    iterator.remove();
                }
            }
        }
    }
}
