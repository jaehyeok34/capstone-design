package capstone.design.topic.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @Override public String name() { return name; }

    @Override
    public int push(Message message) {
        String partition = message.header("partition", "");
        if (partition.isEmpty()) {
            System.err.println("! MemoryTopic.push(): 필수 옵션 누락");
            return -1;
        }

        Map<Integer, TopicRecord> storage = storages.computeIfAbsent(partition, ignored -> {
            return new ConcurrentHashMap<>();
        });

        // 논리 오프셋 할당 및 갱신
        int offset = offsets.getOrDefault(partition, 0);
        offsets.put(partition, offset + 1);

        // 메시지 저장
        storage.put(offset, new TopicRecord(message));

        // 알림 전송
        subscribeManager.notify(partition);

        return offset;
    }

    @Override
    public @Nullable TopicRecord pull(Message message) {
        String partition = message.header("partition", "");
        String clientId = message.header("client.id", "");
        if (partition.isEmpty() || clientId.isEmpty()) {
            System.err.println("! MemoryTopic.pull(): 필수 옵션 누락");
            return null;
        }

        Map<Integer, TopicRecord> storage = storages.get(partition);

        if (storage == null || storage.isEmpty()) {
            System.err.println("! MemoryTopic.pull(): 파티션에 메시지 없음");
            return null;
        }

        /*
         * client offset 획득.
         * 
         * 최초 요청 및 기존 오프셋이 유효하지 않은 경우(기존 오프셋의 메시지가 만료되어 삭제된 경우)
         * defaultOffset(파티션의 최소 offset)으로 설정하여 FIFO 방식으로 메시지를 처리하도록 함.
         * 
         * offset을 획득한 이후에는 곧바로 오프셋을 증가하여 갱신함
         */
        int defaultOffset = Collections.min(storage.keySet());
        int offset = clientOffsets.computeIfAbsent(partition, ignored -> {
            return new ConcurrentHashMap<>();
        }).getOrDefault(clientId, defaultOffset);
        clientOffsets.get(partition).put(clientId, offset + 1);

        /*
         * 메모리 토픽의 경우 메시지 재처리를 지원하지 않기 때문에
         * storage.get()이 아닌, remove()를 통해 메시지를 꺼냄과 동시에 삭제함
         */
        TopicRecord record = storage.remove(offset);
        if (record == null || record.isExpired(retention)) {
            System.err.println("! MemoryTopic.pull(): 유효하지 않은 메시지");
            return null;
        }

        return record;
    }

    @Override
    public boolean seek(Message message) {
        String partition = message.header("partition", "");
        String clientId = message.header("client.id", "");
        int offset = Integer.parseInt(message.header("offset", "-1"));
        if (partition.isEmpty() || clientId.isEmpty() || offset < 0) {
            System.err.println("! MemoryTopic.seek(): 필수 옵션 누락");
            return false;
        }

        clientOffsets.computeIfAbsent(partition, ignored -> {
            return new ConcurrentHashMap<>();
        }).put(clientId, offset);
        
        return true;
    }

    @Override
    public int find(Message message) {
        String partition = message.header("partition", "");
        if (partition.isEmpty()) {
            System.err.println("! MemoryTopic.find(): 필수 옵션 누락");
            return -1;
        }

        Map<Integer, TopicRecord> storage = storages.get(partition);
        if (storage == null || storage.isEmpty()) {
            System.err.println("! MemoryTopic.find(): 빈 파티션");
            return -1;
        }

        // find 조건 추출
        Map<String, String> condition = new HashMap<>();
        for (Map.Entry<String, String> header : message.header().entrySet()) {
            String key = header.getKey();
            if (key.startsWith("condition.")) {
                condition.put(key.substring("condition.".length()), header.getValue());
            }
        }

        // 조건에 맞는 첫 번째 메시지의 offset 반환을 위하여 key(offset)을 기준으로 정렬 후 탐색
        List<Integer> finded = new ArrayList<>();
        for (Map.Entry<Integer, TopicRecord> entry : storage.entrySet()) {
            TopicRecord record = entry.getValue();
            if (record.matches(condition) && !record.isExpired(retention)) {
                finded.add(entry.getKey());
            }
        }

        if (finded.isEmpty()) {
            System.err.println("! MemoryTopic.find(): 탐색 실패");
            return -1;
        }

        return Collections.min(finded);
    }

    @Override
    public int subscribe(Message message, Collection<Object> out) {
        String partition = message.header("partition", "");
        if (partition.isEmpty()) {
            System.err.println("! MemoryTopic.subscribe(): 필수 옵션 누락");
            return -1;
        }

        return subscribeManager.subscribe(partition, out);
    }

    @Override
    public void unsubscribe(Message message, int key) {
        String partition = message.header("partition", "");
        if (partition.isEmpty()) {
            System.err.println("! MemoryTopic.unsubscribe(): 필수 옵션 누락");
            return;
        }
        
        subscribeManager.unsubscribe(partition, key);
    }

    @Override
    public int count(Message message) {
        String partition = message.header("partition", "");
        if (partition.isEmpty()) {
            System.err.println("! MemoryTopic.count(): 필수 옵션 누락");
            return 0;
        }
        
        Map<Integer, TopicRecord> storage = storages.get(partition);
        if (storage == null || storage.isEmpty()) {
            return 0;
        }
        
        return storage.size();
    }

    @Override
    public void clean() {
        for (Map.Entry<String, Map<Integer, TopicRecord>> entry : storages.entrySet()) {
            String partition = entry.getKey();
            Map<Integer, TopicRecord> storage = entry.getValue();

            Iterator<Map.Entry<Integer, TopicRecord>> iterator = storage.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Integer, TopicRecord> recordEntry = iterator.next();
                if (recordEntry.getValue().isExpired(retention)) {
                    iterator.remove();
                    System.out.println("! MemoryTopic.clean(): " + name + "." + partition + "=" + storage.size());
                }
            }
        }
    }
}
