package capstone.design.topic.disk.segment;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import capstone.design.message.Frame;
import capstone.design.message.Message;
import capstone.design.topic.TopicRecord;
import io.netty.buffer.ByteBuf;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;

public class Segment {

    private final int index;
    private final Path log;
    private final Path idx;
    private final int startOffset;
    private final long createdAt;
    private final AtomicInteger endOffset = new AtomicInteger(0);
    private final Object fileLock = new Object();

    public Segment(int index, Path log, Path idx, int startOffset, long createdAt) {
        this.index = index;
        this.log = log;
        this.idx = idx;
        this.startOffset = startOffset;
        this.createdAt = createdAt;
        this.endOffset.set(startOffset + count());
    }

    public int index() { return index; }
    public Path log() { return log; }
    public Path idx() { return idx; }
    public int startOffset() { return startOffset; } 
    public int endOffset() { return endOffset.get(); }
    public boolean isActive(long duration) { return (System.currentTimeMillis() - createdAt) < duration; }
    public boolean isExpired(long retention) { return (System.currentTimeMillis() - createdAt) > retention; }

    public int count() {
        try {
            return (int) Files.size(idx) / Long.BYTES;
        } catch (IOException e) {
            return 0;
        }
    }

    public boolean write(Frame frame) {
        OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND  
        };

        synchronized (fileLock) {
            try (
                FileChannel idxFile = FileChannel.open(idx, options);
                FileChannel logFile = FileChannel.open(log, options);
            ) {
                long position = writeLog(logFile, frame);
                writeIdx(idxFile, position);
                endOffset.incrementAndGet();
                
                return true;
            } catch (IOException e) {
                System.err.println("? Segment.write(): " + e);
                return false;
            }
        }
    }

    public @Nullable TopicRecord read(int offset) {
        synchronized (fileLock) {
            try (
                FileChannel logFile = FileChannel.open(log, StandardOpenOption.READ);
                RandomAccessFile idxFile = new RandomAccessFile(idx.toFile(), "r")
            ) {
                // idx 파일에서 메시지 실제 위치 획득
                idxFile.seek(offset * Long.BYTES);
                long position = idxFile.readLong();
                
                // 실제 위치 기반 메시지 읽기(길이 먼저)
                ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES * 2);
                logFile.read(lengthBuf, position);
                lengthBuf.flip();
                
                // header와 payload 길이 획득
                int headerLength = lengthBuf.getInt();
                int payloadLength = lengthBuf.getInt();
                
                // header 읽기
                long headerPos = position + Integer.BYTES * 2;
                Message.Builder builder = readHeader(logFile, headerPos, headerLength);
                
                // payload 읽기(FileRegion 생성)
                long payloadPos = headerPos + headerLength;
                FileRegion region = new DefaultFileRegion(log.toFile(), payloadPos, payloadLength);
                builder.payload(region);
                
                return new TopicRecord(builder.build());            
            } catch (Exception e) {
                System.err.println("Segment.read(): " + e + " " + idx);
                return null;
            }
        }
    }

    public int find(Map<String, String> condition) {
        int count = count();
        for (int offset = 0; offset < count; offset++) {
            TopicRecord record = read(offset);
            if (record == null || !record.matches(condition)) {
                continue;
            }   

            return offset;
        }

        return -1;
    }

    public boolean clear() {
        try {
            synchronized (fileLock) {
                Files.deleteIfExists(log);
                Files.deleteIfExists(idx);
            }

            return true;
        } catch (IOException e) {
            System.err.println("Segment.clear(): " + e);
            return false;
        }
    }

    public ByteBuffer toBuffer() {
        List<byte[]> pathBytesList = new ArrayList<>();
        for (Path path : new Path[] {log, idx}) {
            pathBytesList.add(path.toString().getBytes(StandardCharsets.UTF_8));
        }

        ByteBuffer buffer = ByteBuffer.allocate(
            (Integer.BYTES * 4) + // index, startOffset, path length * 2
            Long.BYTES +       // createdAt
            pathBytesList.stream().mapToInt(pathBytes -> pathBytes.length).sum() // path bytes lengths
        );

        buffer.putInt(index);
        pathBytesList.forEach(pathBytes -> buffer.putInt(pathBytes.length).put(pathBytes));
        buffer.putInt(startOffset)
            .putLong(createdAt);

        return buffer.flip();
    }

    private long writeLog(FileChannel file, Frame frame) throws IOException {
        /**
         * 다음 쓰기 위치 획득.
         * 논리 offset과 혼동될 수 있는데, 실제 파일에 저장되는 메시지는 rollover 되기 전까지
         * 계속 누적 되므로 int 범위를 초과하는 위치가 될 수 있음.(대용량 메시지가 여러개 쌓이는 경우)
         * 다만, 논리 offset 즉, 실제 메시지의 개수는 int개를 초과하지 않는다고 가정.(retention 정책에 의해 지속적으로 삭제되므로)
         */
        long position = file.position();

        // 메시지 길이 기록
        ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES * 2)
            .putInt(frame.headerLength())
            .putInt(frame.payloadLength());
        file.write(lengthBuf.flip());  

        ByteBuf header = frame.header();
        file.write(header.nioBuffer());
        header.release();

        if (frame.payload() instanceof ByteBuf payload) {
            file.write(payload.nioBuffer());
            payload.release();
        }

        // frame.payload()가 FileRegion일 수도 있는데, segment에 write를 할 때는 그럴 경우가 없을것으로 예상되어 고려하지 않음
        return position;
    }

    private void writeIdx(FileChannel file, long position) throws IOException {
        ByteBuffer offsetBuf = ByteBuffer.allocate(Long.BYTES);
        offsetBuf.putLong(position).flip();

        file.write(offsetBuf);
    }

    private Message.Builder readHeader(FileChannel file, long position, int length) {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        Message.Builder builder = Message.builder();
        try {
            // header bytes 읽기
            file.read(buffer, position);
            buffer.flip();

            // Message Builder 생성 및 type 설정
            builder.type(buffer.get());

            // header key-value 획득
            int headerCount = buffer.get();
            for (int i = 0; i < headerCount; i++) {
                int keyLength = buffer.getShort();
                byte[] keyBytes = new byte[keyLength];
                buffer.get(keyBytes);
                String key = new String(keyBytes, StandardCharsets.UTF_8);
                
                int valueLength = buffer.getInt();
                byte[] valueBytes = new byte[valueLength];
                buffer.get(valueBytes);
                String value = new String(valueBytes, StandardCharsets.UTF_8);

                builder.header(key, value);
            }
        } catch (IOException e) {
            System.err.println("? Segment.readHeader(): " + e);
        }

        return builder;
    }
}
