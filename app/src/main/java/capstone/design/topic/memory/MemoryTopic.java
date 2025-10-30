package capstone.design.topic.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import capstone.design.topic.TopicRecord;
import capstone.design.topic.subscribe.SubscriberManager;
import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.topic.Topic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class MemoryTopic implements Topic {

    private final SubscriberManager manager;
    private final Map<Integer, Map<String, List<MemoryRecord>>> topic = new ConcurrentHashMap<>();

    public MemoryTopic(String name) {
        Utils.validate(name);

        this.manager = new SubscriberManager(name);
    }

    public SubscriberManager subscriberManager() { return manager; }

    /**
     * @param buf 전달 시 반드시 retain() 된 상태(refCnt() > 1) 이어야 함
     * 내부적으로 buf의 참조 카운트를 1 감소시키기 때문.(최소 1은 유지해야 함)
     */
    @Override
    public boolean push(int partition, String clientId, ByteBuf buf) {
        Utils.validate(clientId, buf);

        Map<String, List<MemoryRecord>> partitionMap = topic.computeIfAbsent(
            partition, 
            ignored -> new ConcurrentHashMap<>()
        );

        // partition에 id에 해당하는 큐가 없다면 새롭게 생성
        partitionMap.computeIfAbsent(
            clientId, 
            ignored -> Collections.synchronizedList(new ArrayList<>())
        );

        // partition의 모든 구독자에게 레코드 추가
        for (List<MemoryRecord> storage : partitionMap.values()) {
            storage.add(MemoryRecord.of(buf));
        }
        buf.release();
        
        return true;    
    }
    
    @Override
    public TopicRecord pull(int partition, String clientId, long cursor) {
        Utils.validate(clientId);

        List<MemoryRecord> storage = storage(partition, clientId);
        if (storage == null || storage.isEmpty()) {
            return null;
        }

        /*
         * cursor가 long 타입이긴 하나, 내부적으로는 int 범위 내에서만 동작함
         * int 범위를 넘어가는 경우를 고려하지 않음
         */
        cursor = (cursor < 0) ? 0 : cursor;
        return storage.remove((int) cursor);
    }
    public TopicRecord pull(int partition, String clientId) { return pull(partition, clientId, -1); }

    @Override
    public boolean notify(int partition, Message message) {
        return manager.notify(partition, message);
    }

    @Override
    public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
        // 구독하려는 partition/id에 해당하는 큐가 없다면 새롭게 생성
        topic.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(clientId, ignored -> Collections.synchronizedList(new ArrayList<>()));

        manager.subscribe(context, partition, clientId);
    }

    @Override
    public void unsubscribe(int partition, String clientId) {
        // partition/id에 해당하는 큐 삭제
        Map<String, List<MemoryRecord>> partitionMap = topic.get(partition);
        if (partitionMap != null) {
            partitionMap.remove(clientId);
        }

        manager.unsubscribe(partition, clientId);
    }

    @Override
    public long length(int partition, String clientId) {
        List<MemoryRecord> storage = storage(partition, clientId);
        if (storage == null) {
            return 0;
        }

        return storage.size();
    }

    /**
     * 다음 읽을 위치 반환.
     * 기본적으로 저장소가 List라 하더라도 FIFO 방식으로 동작하기 때문에 항상 0을 반환
     */
    @Override
    public long cursor(int partition, String clientId) {
        return 0;
    }

    /**
     * 다음 저장 위치 반환.
     * 리스트 기반(인덱스 0 시작)이기 때문에 다음 저장 위치 = 길이
     */
    @Override
    public long offset(int partition, String clientId) {
        return length(partition, clientId);
    }
    
    @Override
    public long remainingCount(int partition, String clientId) {
        return length(partition, clientId);
    }

    private List<MemoryRecord> storage(int partition, String clientId) {
        Map<String, List<MemoryRecord>> partitionMap = topic.get(partition);
        if (partitionMap == null) {
            return null;
        }

        return partitionMap.get(clientId);
    }
}
