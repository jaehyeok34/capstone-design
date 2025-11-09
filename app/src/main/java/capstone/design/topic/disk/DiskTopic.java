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

import io.netty.channel.ChannelHandlerContext;

public class DiskTopic implements Topic {

    private static final String DEFAULT_TOPIC_DIR = "./disk_topics";
    private static final long DEFAULT_SEGMENT_DURATION = 10 * (60 * 1000); // 10분
    private static final long DEFAULT_SEGMENT_RETENTION = 30 * (60 * 1000); // 30분

    private final Path root;
    private final SubscriberManager subscriberManager;
    private final long segmentDuration;
    private final long segmentRetention;
    private final Map<Integer, SegmentManager> segmentManagers = new HashMap<>();
    
    private final String name;

    private DiskTopic(String name, long segmentDuration, long segmentRetention) throws IOException {
        Utils.validate(name);

        this.segmentDuration = (segmentDuration >= 0) ? segmentDuration : DEFAULT_SEGMENT_DURATION;
        this.segmentRetention = (segmentRetention >= 0) ? segmentRetention : DEFAULT_SEGMENT_RETENTION;
        this.root = Files.createDirectories(Paths.get(DEFAULT_TOPIC_DIR, name));
        this.subscriberManager = new SubscriberManager();

        this.name = name;

        // 프로그램이 재시작 시 기존에 생성된 세그먼트 매니저들이 있다면, 로드
        loadSegmentManagers();
    }

    public static DiskTopic of(String name) throws IOException {
        return new DiskTopic(name, DEFAULT_SEGMENT_DURATION, DEFAULT_SEGMENT_RETENTION);
    }

    public static DiskTopic of(String name, long segmentDuration, long segmentRetention) throws IOException {
        return new DiskTopic(name, segmentDuration, segmentRetention);
    }

    public SubscriberManager subscriberManager() { return subscriberManager; }
    public SegmentManager segmentManager(int partition) { return segmentManagers.get(partition); }

    /**
     * partition 마다 별도의 segment manager가 존재.
     * segment manager는 여러개의 segment를 관리(segment 생성, 전환 등).
     * buf는 push가 끝나면 모든 refCnt를 감소시킴
     * 
     * @param clientId ignored
     */
    @Override
    public boolean push(int partition, String clientId, byte[] payload) {
        if (!Utils.isValid(payload)) {
            return false;
        }

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

        return segmentManager.write(payload);
    }

    @Override
    public @Nullable TopicRecord pull(int partition, String clientId) {
        return pull(partition, clientId, -1);
    }

    @Nullable
    @Override
    public TopicRecord pull(int partition, String clientId, long offset) {
        if (!Utils.isValid(clientId)) {
            return null;
        }   

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
            return -1;
        }

        return segmentManager.offset(clientId);
    }

    @Override
    public void clean() {
        for (Map.Entry<Integer, SegmentManager> entry : segmentManagers.entrySet()) {
            int partition = entry.getKey();
            SegmentManager segmentManager = entry.getValue();

            segmentManager.clean();
            System.out.println("DiskTopic.clean(): " + name + "." + partition + "=" + segmentManager.messageCount());
        }
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * 토픽의 모든 파일 및 디렉토리 삭제
     */
    public void clearAll() {
        for (SegmentManager segmentManager : segmentManagers.values()) {
            segmentManager.clearAll();
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

        segmentManager.clearAll();
    }

    public int segmentCount(int partition) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return 0;
        }

        return segmentManager.segmentCount();
    }

    private void loadSegmentManagers() {
        for (File dir : root.toFile().listFiles(File::isDirectory)) {
            int key = Integer.parseInt(dir.getName());
            SegmentManager value = new SegmentManager(dir.toPath(), segmentDuration, segmentRetention);

            segmentManagers.put(key, value);
        }
    }
}