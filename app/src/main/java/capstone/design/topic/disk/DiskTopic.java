package capstone.design.topic.disk;

import capstone.design.topic.TopicRecord;
import capstone.design.topic.disk.segment.SegmentManager;
import capstone.design.topic.subscribe.SubscribeManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import capstone.design.message.Message;
import capstone.design.topic.Topic;

public class DiskTopic implements Topic {

    private static final Path TOPIC_DIRECTORY = Path.of("./disk_topics");
    private static final String SEGMENT_MANAGERS_FILE = "segment_managers.log";
    private static final long DEFAULT_DURATION = 10 * (60 * 1000); // 10분
    private static final long DEFAULT_RETENTION = 30 * (60 * 1000); // 30분

    private final String name; 
    private final long duration;
    private final long retention;
    private final Path root;
    private final Map<String, SegmentManager> segmentManagers = new ConcurrentHashMap<>();
    private final SubscribeManager subscribeManager = new SubscribeManager();

    private DiskTopic(String name, long duration, long retention) {
        this.name = name;
        this.duration = duration;
        this.retention = retention;

        this.root = TOPIC_DIRECTORY.resolve(name);

        loadSegmentManagers();
    }

    public static DiskTopic of(String name) { return new DiskTopic(name, DEFAULT_DURATION, DEFAULT_RETENTION); }
    public static DiskTopic of(String name, long duration, long retention) { return new DiskTopic(name, duration, retention); }

    @Override
    public String name() { return name; }

    @Override
    public int push(String partition, Message message) {
        SegmentManager segmentManager = segmentManagers.computeIfAbsent(partition, ignored -> {
            try {
                SegmentManager newSegmentManager = new SegmentManager(root.resolve(partition), duration, retention);

                if (!appendSegmentManager(newSegmentManager)) {
                    return null;
                }
                
                return newSegmentManager;
            } catch (IOException e) {
                System.err.println("? DiskTopic.push(): " + e);
                return null;
            }
        });
        
        if (segmentManager == null) {
            return -1;
        }

        return segmentManager.write(message);
    }

    @Override
    public @Nullable TopicRecord peek(String partition, String clientId, Message message) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return null;
        }

        return segmentManager.peek(clientId);
    }

    @Override
    public void commit(String partition, String clientId, int offset, Message message) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return;
        }

        segmentManager.commit(clientId, offset);
    }

    @Override
    public int find(String partition, Map<String, String> condition, Message message) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return -1;
        }

        return segmentManager.find(condition);
    }

    @Override
    public boolean seek(String partition, String clientId, int offset, Message message) {
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return false;
        }

        return segmentManager.seek(clientId, offset);
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
        SegmentManager segmentManager = segmentManagers.get(partition);
        if (segmentManager == null) {
            return -1;
        }

        return segmentManager.count();
    }

    @Override
    public void clean() {
        segmentManagers.values().forEach(segmentManger -> {
            segmentManger.clean();
        });
    }

    public void clearAll() {
        segmentManagers.values().forEach(segmentaManager -> {
            segmentaManager.clearAll();
        });

        try {
            Files.deleteIfExists(root.resolve(SEGMENT_MANAGERS_FILE));
            Files.deleteIfExists(root);
        } catch (IOException e) {
            System.err.println("? DiskTopic.clearAll(): " + e);
        }
    }

    private boolean appendSegmentManager(SegmentManager segmentManager) {
        OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND
        };

        try (FileChannel file = FileChannel.open(root.resolve(SEGMENT_MANAGERS_FILE), options)) {
            byte[] pathBytes = segmentManager.root().getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + pathBytes.length);
            buffer.putInt(pathBytes.length)
                .put(pathBytes);
                 
            file.write(buffer.flip());

            return true;
        } catch (IOException e) {
            System.err.println("? DiskTopic.appendSegmentManager(): " + e);
            return false;
        }
    }

    private boolean loadSegmentManagers() {
        try (FileChannel file = FileChannel.open(root.resolve(SEGMENT_MANAGERS_FILE), StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate((int) file.size());
            while (buffer.hasRemaining()) {
                if (file.read(buffer) == -1) {
                    break;
                }
            }

            buffer.flip();
            while (buffer.hasRemaining()) {
                int pathLength = buffer.getInt();
                byte[] pathBytes = new byte[pathLength];
                buffer.get(pathBytes);

                Path path = Path.of(new String(pathBytes, StandardCharsets.UTF_8));
                SegmentManager segmentManager = new SegmentManager(path, duration, retention);

                segmentManagers.put(path.getFileName().toString(), segmentManager);
            }

            return true;
        } catch (IOException e) {
            System.err.println("? DiskTopic.loadSegmentManagers(): " + e);
            return false;
        }
    }
}