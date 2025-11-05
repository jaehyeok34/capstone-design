package capstone.design.topic.disk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import capstone.design.topic.TopicRecord;
import capstone.design.topic.disk.segment.SegmentManager;
import capstone.design.topic.subscribe.SubscriberManager;
import org.jspecify.annotations.Nullable;
import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.topic.Topic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class DiskTopic implements Topic {

    private static final String DEFAULT_TOPIC_DIR = "./topic";
    private static final long DEFAULT_SEGMENT_DURATION = 60 * 10 * 1000; // 10분
    private static final long DEFAULT_SEGMENT_RETENTION = 60 * 10 * 1000; // 10분

    private final Path root;
    private final SubscriberManager subscriberManager;
    private final long segmentDuration;
    private final long segmentRetention;
    private final Map<Integer, SegmentManager> segmentManagers = new HashMap<>();

    public DiskTopic(String name, long segmentDuration, long segmentRetention) throws IOException {
        Utils.validate(name);

        this.segmentDuration = (segmentDuration >= 0) ? segmentDuration : DEFAULT_SEGMENT_DURATION;
        this.segmentRetention = (segmentRetention >= 0) ? segmentRetention : DEFAULT_SEGMENT_RETENTION;
        this.root = Files.createDirectories(Paths.get(DEFAULT_TOPIC_DIR, name));
        this.subscriberManager = new SubscriberManager(name);

        // 프로그램이 재시작 시 기존에 생성된 세그먼트 매니저들이 있다면, 로드
        loadSegmentManagers();
    }

    public DiskTopic(String name) throws IOException {
        this(name, DEFAULT_SEGMENT_DURATION, DEFAULT_SEGMENT_RETENTION);
    }

    public SubscriberManager subscriberManager() { return subscriberManager; }
    public SegmentManager segmentManager(int partition) { return segmentManagers.get(partition); }

    /**
     * partition 마다 별도의 segment manager가 존재.
     * segment manager는 여러개의 segment를 관리(segment 생성, 전환 등).
     * buf는 push가 끝나면 모든 refCnt를 감소시킴
     */
    @Override
    public boolean push(int partition, String clientId, ByteBuf buf) {
        // partition 디렉토리 생성(이미 존재한다면 무시)
        Path dir;
        try {
            dir = Files.createDirectories(root.resolve(String.valueOf(partition)));
        } catch (IOException e) {
            System.err.println("DiskTopic.push(): " + e);
            return false;
        }

        // partition에 해당하는 segment manager 획득(없으면 생성)
        SegmentManager segmentManager = segmentManagers.computeIfAbsent(
            partition, 
            ignored -> new SegmentManager(dir, segmentDuration, segmentRetention)
        ); 

        boolean ok = segmentManager.write(buf);
        buf.release(buf.refCnt());

        return ok;
    }

    @Nullable
    @Override
    public TopicRecord pull(int partition, String clientId, long offset) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return null;
        }

        return segmentManager.read(clientId, offset);
    }

    @Override
    public boolean notify(int partition, Message message) {
        return subscriberManager.notify(partition, message);
    }

    @Override
    public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
        subscriberManager.subscribe(context, partition, clientId);
    }

    @Override
    public void unsubscribe(int partition, String clientId) {
        subscriberManager.unsubscribe(partition, clientId);
    }

    @Override
    public long count(int partition, String clientId) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return 0;
        }

        return segmentManager.messageCount();
    }

    @Override
    public long offset(int partition, String clientId) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return 0;
        }

        return segmentManager.offset(clientId);
    }

    /**
     * 토픽의 모든 파일 및 디렉토리 삭제
     */
    public void clearAll() {
        for (SegmentManager segmentManager : segmentManagers.values()) {
            segmentManager.clear();
        }

        try {
            Files.deleteIfExists(root);
        } catch (IOException e) {
            System.err.println("DiskTopic.clearAll(): " + e);   
        }
    }

    /**
     * 특정 파티션의 모든 파일 삭제
     */
    public void clear(int partition) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return;
        }

        segmentManager.clear();
    }

    public int segmentCount(int partition) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return 0;
        }

        return segmentManager.segmentCount();
    }

    private void loadSegmentManagers() {
        for (File dir : root.toFile().listFiles()) {
            int key = Integer.parseInt(dir.getName());
            SegmentManager value = new SegmentManager(dir.toPath(), segmentDuration, segmentRetention);

            segmentManagers.put(key, value);
        }
    }
}
