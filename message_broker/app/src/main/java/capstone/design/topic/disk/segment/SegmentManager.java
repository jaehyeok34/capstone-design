package capstone.design.topic.disk.segment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import capstone.design.message.Message;
import capstone.design.topic.TopicRecord;

public class SegmentManager {

    private static final String SEGMENTS_FILE = "segments.log";
    private static final String CLIENT_OFFSETS_FILE = "client_offsets.log";
    private static final String LOG_FILE_EXTENTION = ".log";
    private static final String IDX_FILE_EXTENTION = ".idx";

    private final Path root;
    private final List<Segment> segments = Collections.synchronizedList(new ArrayList<>());
    private final long duration; // 세그먼트 롤오버 기간(ms)
    private final long retention; // 세그먼트 보관 기간(ms)
    private Segment activeSegment;
    private final AtomicInteger segmentIndex = new AtomicInteger(0);
    private final AtomicInteger nextOffset = new AtomicInteger(0);
    private final Map<String, Integer> clientOffsets = new ConcurrentHashMap<>();
    private FileChannel clientOffsetsFile;

    public SegmentManager(Path root, long duration, long retention) throws IOException {
        this.root = root;
        this.duration = duration;
        this.retention = retention;

        Files.createDirectories(root);

        loadSegments();
        loadClientOffsets();
    }

    public String root() { return root.toString(); }

    public int write(Message message) {
        if (activeSegment == null || !activeSegment.isActive(duration)) {
            rollover();
        }

        message.addHeader("offset", String.valueOf(nextOffset.get()));
        if (!activeSegment.write(message.toFrame())) {
            return -1;
        }

        return nextOffset.getAndIncrement();
    }

    public @Nullable TopicRecord peek(String clientId) {
        List<Segment> validSegments = segments.stream()
            .filter(segment -> activeSegment == segment || !segment.isExpired(retention))
            .toList();

        int defaultOffset = validSegments.stream()
            .mapToInt(Segment::startOffset) // 유효한 세그먼트들의 start offset
            .min() // 중 최소값
            .orElse(0); // 없으면 0

        /**
         * client id에 해당하는 offset이 없다면, default offset으로 설정
         * 있다면, default offset과 비교하여 더 큰 값으로 설정
         * default offset > offset인 경우는 세그먼트가 만료되어 기존 offset이 유효하지 않은 경우.
         */
        int clientOffset = clientOffsets.compute(clientId, (ignored, offset) -> {
            return offset == null ? defaultOffset : Math.max(offset, defaultOffset);
        });

        return validSegments.stream()
            .filter(segment -> clientOffset >= segment.startOffset() && clientOffset < segment.endOffset())
            .findFirst()
            .map(segment -> segment.read(clientOffset - segment.startOffset()))
            .orElse(null);
    }

    public void commit(String clientId, int offset) {
        int updatedOffset = offset + 1;

        clientOffsets.put(clientId, updatedOffset); // 메모리에 반영
        appendClientOffset(clientId, updatedOffset); // 파일에 반영
    }

    public int find(Map<String, String> condition) {
        for (Segment segment: segments) {
            if (activeSegment != segment && segment.isExpired(retention)) {
                continue;
            }

            int offset = segment.find(condition);
            if (offset >= 0) {
                return segment.startOffset() + offset;
            }
        }

        return -1;
    }

    public boolean seek(String clientId, int offset) {
        if (offset < 0 || offset >= nextOffset.get()) {
            return false;
        }

        clientOffsets.put(clientId, offset);

        return true;
    }

    /**
     * 유효한 segment 들의 메시지의 합 반환.
     * active segment는 만료되더라도 유효하다고 판단.
     */
    public int count() {
        return segments.stream()
            .filter(segment -> activeSegment == segment || !segment.isExpired(retention))
            .mapToInt(Segment::count)
            .sum();
    }

    /**
     * retention 기준으로 만료된 세그먼트 캐시를 정리.
     * 단, active 세그먼트는 정리하지 않으며 segmsnts.log 파일은 갱신하지 않음(메모리만 정리).
     * segments.log 파일을 갱신하게 될 경우 clean interval이 짧을 경우 I/O가 자주 발생할 수 있기 때문.
     */
    public void clean() {
        Iterator<Segment> iterator = segments.iterator();
        while (iterator.hasNext()) {
            Segment segment = iterator.next();
            if (segment != activeSegment && segment.isExpired(retention)) {
                segment.clear(); // log, idx 파일 삭제
                iterator.remove();
            }
        }
    }

    public boolean clearAll() {
        try {
            // 모든 세그먼트 파일 삭제
            for (Segment segment: segments) {
                segment.clear();
            }

            segments.clear();

            // 메타 파일 및 오프셋 파일 삭제
            Files.deleteIfExists(root.resolve(SEGMENTS_FILE));
            Files.deleteIfExists(root.resolve(CLIENT_OFFSETS_FILE));

            // 루트 디렉토리 삭제
            Files.deleteIfExists(root);

            return true;
        } catch (Exception e) {
            System.err.println("SegmentManager.clearAll(): " + e);
            return false;
        }
    }

    private void rollover() {
        int index = segmentIndex.getAndIncrement();
        Path log = root.resolve(index + LOG_FILE_EXTENTION);
        Path idx = root.resolve(index + IDX_FILE_EXTENTION);
        int startOffset = nextOffset.get();

        activeSegment = new Segment(index, log, idx, startOffset, System.currentTimeMillis());
        segments.add(activeSegment);

        appendActiveSegment();
    }

    private boolean appendActiveSegment() {
        OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND  
        };

        try (FileChannel segmentsFile = FileChannel.open(root.resolve(SEGMENTS_FILE), options)) {
            segmentsFile.write(activeSegment.toBuffer());
            return true;
        } catch (Exception e) {
            System.err.println("? SegmentManager.appendActiveSegment(): " + e);
            return false;
        }
    }

    private boolean appendClientOffset(String clientId, int offset) {
        OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND
        };

        try {
            if (clientOffsetsFile == null) {
                clientOffsetsFile = FileChannel.open(root.resolve(CLIENT_OFFSETS_FILE), options);
            }

            byte[] clientIdBytes = clientId.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate((Integer.BYTES * 2) + clientIdBytes.length);
            buffer.putInt(clientIdBytes.length)
                .put(clientIdBytes)
                .putInt(offset);
            clientOffsetsFile.write(buffer.flip());

            return true;
        } catch (Exception e) {
            System.err.println("? SegmentManager.appendClientOffset(): " + e);
            return false;
        }
    }

    private boolean updateSegments() {
        OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
        };

        try (FileChannel segmentsFile = FileChannel.open(root.resolve(SEGMENTS_FILE), options)) {
            for (Segment segment : segments) {
                segmentsFile.write(segment.toBuffer());
            }

            return true;
        } catch (Exception e) {
            System.err.println("? SegmentManager.updateSegments(): " + e);
            return false;
        }
    }

    private boolean updateClientOffsets() {
        OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING
        };

        try (FileChannel file = FileChannel.open(root.resolve(CLIENT_OFFSETS_FILE), options)) {
            for (Map.Entry<String, Integer> entry : clientOffsets.entrySet()) {
                byte[] clientIdBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.allocate((Integer.BYTES * 2) + clientIdBytes.length);
                buffer.putInt(clientIdBytes.length)
                    .put(clientIdBytes)
                    .putInt(entry.getValue());
                file.write(buffer.flip());
            }

            return true;
        } catch (Exception e) {
            System.err.println("? SegmentManager.updateClientOffsets(): " + e);
            return false;
        }
    }

    private boolean loadSegments() {
        ByteBuffer buffer = loadFile(root.resolve(SEGMENTS_FILE));
        if (buffer == null) {
            return false;
        }

        while (buffer.hasRemaining()) {
            int index = buffer.getInt();
            Path[] paths = new Path[2];
            for (int i = 0; i < 2; i++) {
                int length = buffer.getInt();
                byte[] pathBytes = new byte[length];
                buffer.get(pathBytes);
                paths[i] = Path.of(new String(pathBytes, StandardCharsets.UTF_8));
            }
            int satrtOffset = buffer.getInt();
            long createdAt = buffer.getLong();

            /**
             * 만료된 세그먼트는 복원하지 않고 파일도 제거함.
             * 단, next offset은 갱신 함.
             */
            Segment segment = new Segment(index, paths[0], paths[1], satrtOffset, createdAt);
            segmentIndex.set(index + 1);
            nextOffset.set(Math.max(nextOffset.get(), segment.endOffset()));

            if (segment.isExpired(retention)) {
                segment.clear();
                continue;
            }
            
            segments.add(segment);
        }
        
        // 유효한 세그먼트를 기준으로 segments.log 파일 갱신
        updateSegments();

        return true;
    }

    private boolean loadClientOffsets() {
        ByteBuffer buffer = loadFile(root.resolve(CLIENT_OFFSETS_FILE));
        if (buffer == null) {
            return false;
        }

        while (buffer.hasRemaining()) {
            int clientIdLength = buffer.getInt();
            byte[] clientIdBytes = new byte[clientIdLength];
            buffer.get(clientIdBytes);
            String clientId = new String(clientIdBytes, StandardCharsets.UTF_8);
            int offset = buffer.getInt();

            clientOffsets.put(clientId, offset);
        }

        clientOffsets.entrySet().stream()
            .filter(entry -> entry.getValue() > nextOffset.get()) 
            .forEach(entry -> {
                entry.setValue(nextOffset.get());
            });

        updateClientOffsets();

        return true;
    }

    private @Nullable ByteBuffer loadFile(Path path) {
        try (FileChannel file = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate((int) file.size());
            while (buffer.hasRemaining()) {
                if (file.read(buffer) == -1) {
                    break;
                }
            }

            buffer.flip();
            return buffer;
        } catch (Exception e) {
            System.err.println("? SegmentManager.loadFile(): " + e);
            return null;
        }
    }
}
