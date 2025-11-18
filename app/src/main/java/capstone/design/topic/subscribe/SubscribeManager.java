package capstone.design.topic.subscribe;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SubscribeManager {

    // 외부 키: partition, 내부 키: 자동 할당, Queue<Object>: 단순 알림 큐
    private final Map<String, Map<Integer, Collection<Object>>> subscribeMap = new ConcurrentHashMap<>();
    private final AtomicInteger key = new AtomicInteger(0);

    public int subscribe(String partition, Collection<Object> out) {
        int key = this.key.getAndIncrement();
        subscribeMap.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>())
            .put(key, out);

        return key;
    }

    public void unsubscribe(String partition, int key) {
        Map<Integer, Collection<Object>> subscribers = subscribeMap.get(partition);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(key);
    }

    public void notify(String partition) {
        Map<Integer, Collection<Object>> subscribers = subscribeMap.get(partition);
        if (subscribers == null) {
            return;
        }

        for (Collection<Object> subscriber : subscribers.values()) {
            subscriber.add(true);
        }
    }
}
