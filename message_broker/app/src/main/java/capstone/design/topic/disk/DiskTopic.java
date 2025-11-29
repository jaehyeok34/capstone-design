package capstone.design.topic.disk;

import capstone.design.topic.TopicRecord;
import capstone.design.topic.disk.segment.SegmentManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import capstone.design.message.Message;
import capstone.design.topic.Topic;

public class DiskTopic implements Topic {

    private static final String DEFAULT_TOPIC_DIR = "./disk_topics";
    private static final long DEFAULT_DURATION = 10 * (60 * 1000); // 10분
    private static final long DEFAULT_RETENTION = 30 * (60 * 1000); // 30분

    private final String name; 
    private final long duration;
    private final long retention;
    private final Map<String, SegmentManager> segmentManagers = new ConcurrentHashMap<>();

    private DiskTopic(String name, long duration, long retention) {
        this.name = name;
        this.duration = duration;
        this.retention = retention;
    }

    public static DiskTopic of(String name) {
        return new DiskTopic(name, DEFAULT_DURATION, DEFAULT_RETENTION);
    }

    public static DiskTopic of(String name, long duration, long retention) {
        return new DiskTopic(name, duration, retention);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int push(String partition, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'push'");
    }

    @Override
    public @Nullable TopicRecord peek(String partition, String clientId, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'peek'");
    }

    @Override
    public void commit(String partition, String clientId, int offset, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'commit'");
    }

    @Override
    public int find(String partition, Map<String, String> condition, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'find'");
    }

    @Override
    public boolean seek(String partition, String clientId, int offset, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'seek'");
    }

    @Override
    public int subscribe(String partition, Supplier<Boolean> callback) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'subscribe'");
    }

    @Override
    public void unsubscribe(String partition, int key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unsubscribe'");
    }

    @Override
    public int count(String partition, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'count'");
    }

    @Override
    public void clean() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clean'");
    }




    // // field =====


    // private final Path rootDir;
    // private final Path partitionsDir;
    // private final long duration;
    // private final long retention;
    // // private final SubscribeManager subscribeManager = new SubscribeManager();
    // private final Map<String, SegmentManager> segmentManagers = new HashMap<>();
    
    // private final String name;

    // // constructor =====
    // private DiskTopic(String name, long duration, long retention) throws IOException {
    //     this.duration = duration;
    //     this.retention = retention;

    //     this.rootDir = Files.createDirectories(Paths.get(DEFAULT_TOPIC_DIR, name));
    //     this.partitionsDir = Files.createDirectories(rootDir.resolve("partitions"));

    //     this.name = name;
    // }

    // // static method =====
    // public static DiskTopic of(String name) throws IOException {
    //     return new DiskTopic(name, DEFAULT_DURATION, DEFAULT_RETENTION);
    // }

    // public static DiskTopic of(String name, long segmentDuration, long segmentRetention) throws IOException {
    //     return new DiskTopic(name, segmentDuration, segmentRetention);
    // }

    // // getter =====
    // public SegmentManager segmentManager(String partition) { return segmentManagers.get(partition);  }

    // // override =====
    // @Override
    // public int push(Message message) {
    //     String partition = message.header("partition", "");
    //     if (partition.isEmpty()) {
    //         System.err.println("! DiskTopic.push(): 필수 옵션 누락");
    //         return -1;
    //     }
        
    //     try {
    //         Path dir = Files.createDirectories(partitionsDir.resolve(partition));
            
    //         return 0;
    //     } catch (Exception e) {
    //         System.err.println("! DiskTopic.push(): " + e);
    //         return -1;
    //     }
    // }
    // // @Override
    // // public boolean push(Message message) {
    // //     String partition = message.header("partition", "");
    // //     if (partition.isEmpty()) {
    // //         System.err.println("! DiskTopic.push(): 필수 옵션 누락");
    // //         return false;
    // //     }

    // //     try {
    // //         Path dir = Files.createDirectories(root.resolve(partition));
    // //         SegmentManager segmentManager = segmentManagers.computeIfAbsent(partition, ignored -> {
    // //             return new SegmentManager(dir, duration, retention);
    // //         });

    // //         boolean ok = segmentManager.write(message);


    // //         return ok;
    // //     } catch (Exception e) {
    // //         System.err.println("!DiskTopic.push(): " + e);
    // //         return false;
    // //     }
    // // }

    // @Override
    // public @Nullable TopicRecord pull(int partition, String clientId) {
    //     return pull(partition, clientId, -1);
    // }

    // @Nullable
    // @Override
    // public TopicRecord pull(int partition, String clientId, long offset) {
    //     if (!Utils.isValid(clientId)) {
    //         return null;
    //     }   

    //     SegmentManager segmentManager = segmentManagers.get(partition);
    //     if (segmentManager == null) {
    //         return null;
    //     }

    //     return segmentManager.read(clientId, offset);
    // }

    // @Override
    // public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
    //     subscriberManager.registry(context, partition, clientId);
    // }

    // @Override
    // public void unsubscribe(int partition, String clientId) {
    //     subscriberManager.unsubscribe(partition, clientId);
    // }

    // @Override
    // public long count(int partition, String clientId) {
    //     SegmentManager segmentManager = segmentManagers.get(partition);
    //     if (segmentManager == null) {
    //         return 0;
    //     }

    //     return segmentManager.messageCount();
    // }

    // @Override
    // public long offset(int partition, String clientId) {
    //     SegmentManager segmentManager = segmentManagers.get(partition);
    //     if (segmentManager == null) {
    //         return -1;
    //     }

    //     return segmentManager.offset(clientId);
    // }

    // @Override
    // public void clean() {
    //     for (Map.Entry<Integer, SegmentManager> entry : segmentManagers.entrySet()) {
    //         int partition = entry.getKey();
    //         SegmentManager segmentManager = entry.getValue();

    //         segmentManager.clean();
    //         System.out.println("DiskTopic.clean(): " + name + "." + partition + "=" + segmentManager.messageCount());
    //     }
    // }

    // @Override
    // public String name() {
    //     return name;
    // }

    // // method =====
    // /**
    //  * 토픽의 모든 파일 및 디렉토리 삭제
    //  */
    // public void clearAll() {
    //     for (SegmentManager segmentManager : segmentManagers.values()) {
    //         segmentManager.clearAll();
    //     }

    //     try {
    //         Files.deleteIfExists(rootDir);
    //     } catch (IOException e) {
    //         System.err.println("DiskTopic.clearAll(): " + e);   
    //     }
    // }

    // /**
    //  * 특정 파티션의 모든 파일 삭제
    //  */
    // public void clear(int partition) {
    //     SegmentManager segmentManager = segmentManagers.get(partition);
    //     if (segmentManager == null) {
    //         return;
    //     }

    //     segmentManager.clearAll();
    // }

    // public int segmentCount(int partition) {
    //     SegmentManager segmentManager = segmentManagers.get(partition);
    //     if (segmentManager == null) {
    //         return 0;
    //     }

    //     return segmentManager.segmentCount();
    // }

    // private void loadSegmentManagers() {
    //     for (File dir : rootDir.toFile().listFiles(File::isDirectory)) {
    //         int key = Integer.parseInt(dir.getName());
    //         SegmentManager value = new SegmentManager(dir.toPath(), duration, retention);

    //         segmentManagers.put(key, value);
    //     }
    // }
}