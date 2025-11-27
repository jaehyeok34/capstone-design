package capstone.design.topic.subscribe;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class SubscribeManager {

    private final Map<String, Queue<Integer>> partitionKeys = new ConcurrentHashMap<>();
    private final Map<Integer, Supplier<Boolean>> subscribes = new ConcurrentHashMap<>();
    private final AtomicInteger key = new AtomicInteger(0);

    public int subscribe(String partition, Supplier<Boolean> callback) {
        int key = this.key.getAndIncrement();

        // 키 맵에 추가
        partitionKeys.computeIfAbsent(partition, ignored -> {
            return new ConcurrentLinkedQueue<>();
        }).add(key);

        // 콜백 맵에 추가
        subscribes.put(key, callback);

        log("subscribe");

        return key;
    }

    public void unsubscribe(String partition, int key) {
        // key 맵에서 제거(없다면, 아무 일도 하지 않음)
        partitionKeys.computeIfPresent(partition, (ignored, keys) -> {
            keys.remove(key);
            if (keys.isEmpty()) {
                return null;
            }

            return keys;
        });

        // 콜백 맵에서 제거(없다면, 아무 일도 하지 않음)
        subscribes.remove(key);

        log("unsubscribe");
    }

    public void notify(String partition) {
        partitionKeys.computeIfPresent(partition, (ignored, keys) -> {
            Integer key;
            while ((key = keys.poll()) != null) {
                Supplier<Boolean> callback = subscribes.remove(key);
                if (callback.get()) {
                    return keys; // 메시지 처리 완료, 남은 키들 반환
                }
            }

            return null; // keys에 아무것도 없으므로 맵에서 제거
        });

        log("notify");
    }

    private void log(String caller) {
        System.out.println(
            "! === SubscribeManager 상태(" + caller + ") ===" + "\n" + 
            "partitionKeys: " + partitionKeys + "\n" + 
            // "subscribes: " + subscribes + "\n" +
            " next key: " + key.get()
        );
    }
}
