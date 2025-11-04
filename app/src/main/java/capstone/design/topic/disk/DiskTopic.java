package capstone.design.topic.disk;

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

    private final String name;
    private final Path root;
    private final SubscriberManager manager;
    private final long segmentDuration;
    private final Map<Integer, SegmentManager> partition = new HashMap<>();

    public DiskTopic(String name, long segmentDuration) throws IOException {
        Utils.validate(name);      

        this.segmentDuration = (segmentDuration >= 0) ? segmentDuration : DEFAULT_SEGMENT_DURATION;
        this.name = name;
        this.root = Files.createDirectories(Paths.get(DEFAULT_TOPIC_DIR, name));
        this.manager = new SubscriberManager(name);
    }

    public DiskTopic(String name) throws IOException {
        this(name, DEFAULT_SEGMENT_DURATION);
    }

    public SubscriberManager subscriberManager() { return manager; }
    public SegmentManager segmentManager(int partition) { return this.partition.get(partition); }

    public long messageCount(int partition) {
        SegmentManager segmentManager = this.partition.get(partition);
        if (segmentManager == null) {
            return 0;
        }

        return segmentManager.messageCount();
    }

    public int segmentCount(int partition) {
        SegmentManager segmentManager = this.partition.get(partition);
        if (segmentManager == null) {
            return 0;
        }

        return segmentManager.segmentCount();
    }

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
        SegmentManager segmentManager = this.partition.computeIfAbsent(
            partition, 
            ignored -> new SegmentManager(dir, this.name + "_" + partition, segmentDuration)
        ); 

        boolean ok = segmentManager.write(buf);
        buf.release(buf.refCnt());

        return ok;
    }

    @Nullable
    @Override
    public TopicRecord pull(int partition, String clientId, long offset) {
        SegmentManager segmentManager = this.partition.get(partition);
        if (segmentManager == null) {
            return null;
        }

        return segmentManager.read(clientId, offset);
    }

    @Override
    public boolean notify(int partition, Message message) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'notify'");
    }

    @Override
    public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'subscribe'");
    }

    @Override
    public void unsubscribe(int partition, String clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'unsubscribe'");
    }

    @Override
    public long length(int partition, String clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'length'");
    }

    @Override
    public long cursor(int partition, String clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'cursor'");
    }

    @Override
    public long offset(int partition, String clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'offset'");
    }

    @Override
    public long remainingCount(int partition, String clientId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remainingCount'");
    }

    /**
     * 토픽의 모든 파일 및 디렉토리 삭제
     */
    public void clearAll() {
        for (SegmentManager segmentManager : partition.values()) {
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
        SegmentManager segmentManager = this.partition.get(partition);
        if (segmentManager == null) {
            return;
        }

        segmentManager.clear();
    }

    // /**
    //  * @param clientId 메서드 시그니처를 유지하기 위한 값으로 실제로는 사용하지 않음
    //  */
    // @Override
    // public TopicRecord pull(int partition, String clientId, long cursor) {
    //     FileGroup fileGroup = fileGroup(partition);
    //     if (fileGroup == null) {
    //         System.err.println("[debug] DiskTopic.pull() - [" + partition + "] 파티션 없음");
    //         return null;
    //     }
        
    //     try (
    //         RandomAccessFile offsetFile = new RandomAccessFile(fileGroup.offset().toFile(), "r");
    //     ) {
    //         // cursor 획득
    //         cursor = (cursor < 0) ? readCursor(fileGroup) : cursor;
    //         if (cursor > length(partition) - 1) {
    //             /*
    //              * cursor는 0부터 시작하므로, length - 1보다 크면 읽을 수 있는 메시지 없음
    //              * ex) length = 1이면, cursor는 0까지만 유효
    //              */
    //             throw new IllegalStateException("cursor 읽을 수 있는 메시지 범위 초과");
    //         }
            
    //         // offset 획득
    //         offsetFile.seek(cursor * Long.BYTES);
    //         long offset = offsetFile.readLong();
    //         if (offset < 0) {
    //             throw new IllegalStateException("범위 초과");
    //         }

    //         // message 획득
    //         FileChannel logFile = FileChannel.open(fileGroup.log(), StandardOpenOption.READ);
    //         ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
    //         logFile.read(lengthBuf, offset);
    //         lengthBuf.flip();
    //         int length = lengthBuf.getInt();
            
    //         // DiskRecord 생성
    //         // offset + Integer.BYTES: 메시지 길이 정보는 읽었으니 건너 뛰고 실제 메시지부터 읽기 위함
    //         DiskRecord record = DiskRecord.of(logFile, offset + Integer.BYTES, length);
            
    //         // cursor 갱신
    //         updateCursor(fileGroup, cursor + 1);
            
    //         return record;
    //     } catch (Exception e) {
    //         System.err.println("[debug] DiskTopic.pull() - [" + partition + "] 메시지 읽기 실패"); 
    //         e.printStackTrace();

    //         return null;
    //     }
    // }
    // public TopicRecord pull(int partition, long cursor) { return pull(partition, null, cursor); }
    // public TopicRecord pull(int partition) { return pull(partition, -1); }

    // @Override
    // public boolean notify(int partition, Message message) {
    //     return manager.notify(partition, message);
    // }
        
    // @Override
    // public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
    //     manager.subscribe(context, partition, clientId);
    // }

    // @Override
    // public void unsubscribe(int partition, String clientId) {
    //     manager.unsubscribe(partition, clientId);
    // }

    
    // /**
    //  * @param clientId memory topic과 동일한 메서드 시그니처 유지를 위해 남겨둠.
    //  * 실제로는 사용하지 않음
    //  */
    // @Override
    // public long length(int partition, String clientId) { 
    //     FileGroup fileGroup = fileGroup(partition);
    //     if (fileGroup == null) {
    //         return 0L;
    //     }

    //     return fileGroup.offset().toFile().length() / Long.BYTES;
    // }
    // public long length(int partition) { return length(partition, null); }

    // @Override
    // public long cursor(int partition, String clientId) {
    //     FileGroup fileGroup = fileGroup(partition);
    //     if (fileGroup == null) {
    //         return 0;
    //     }

    //     return readCursor(fileGroup);
    // }
    // public long cursor(int partition) { return cursor(partition, null); }

    // /**
    //  * disk topic에서는 offset과 length가 동일함(메시지가 삭제되지 않으므로)
    //  */
    // @Override
    // public long offset(int partition, String clientId) {
    //     return length(partition);
    // }
    // public long offset(int partition) { return offset(partition, null); }

    // /**
    //  * disk topic에서는 남은 메시지 수가 length와 동일함(메시지가 삭제되지 않으므로)
    //  */
    // @Override
    // public long remainingCount(int partition, String clientId) {
    //     return length(partition);
    // }
    // public long remainingCount(int partition) { return remainingCount(partition, null); }
    
    // @Nullable
    // private FileGroup fileGroup(int partition) {
    //     Path path = root.resolve(String.valueOf(partition));
    //     if (Files.notExists(path)) {
    //         return null;
    //     }

    //     return new FileGroup(path, name);
    // }

    // private long writeLog(ByteBuf buf, FileChannel file) throws IOException {
    //     // 메시지 offset 획득
    //     long offset = file.position();

    //     // 메시지 길이 기록
    //     ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
    //     lengthBuf.putInt(buf.readableBytes());
    //     lengthBuf.flip();
    //     file.write(lengthBuf);

    //     // 메시지 내용 기록
    //     file.write(buf.nioBuffer());

    //     return offset;
    // }

    // private void writeOffset(long offset, FileChannel file) throws IOException {
    //     ByteBuffer offsetBuf = ByteBuffer.allocate(Long.BYTES);
    //     offsetBuf.putLong(offset);
    //     offsetBuf.flip();
    //     file.write(offsetBuf);
    // }

    // private long readCursor(FileGroup fileGroup) {
    //     try (
    //         FileChannel cursorFileChannel = FileChannel.open(fileGroup.cursor(),
    //             StandardOpenOption.CREATE, StandardOpenOption.READ);
    //     ) {
    //         ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    //         int read = cursorFileChannel.read(buffer);
    //         if (read != Long.BYTES) {
    //             return 0; // 커서 파일이 비어있으면 0으로 간주
    //         }

    //         buffer.flip();
    //         return buffer.getLong();
    //     } catch (IOException e) {
    //         System.err.println("[debug] DiskTopic.readCursor(): " + e);
    //         return 0;
    //     }
    // }

    // private void updateCursor(FileGroup fileGroup, long cursor) {
    //     try (
    //         FileChannel cursorFileChannel = FileChannel.open(fileGroup.cursor(),
    //             StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    //     ) {
    //         ByteBuffer cursorBuf = ByteBuffer.allocate(Long.BYTES);
    //         cursorBuf.putLong(cursor);
    //         cursorBuf.flip();
    //         cursorFileChannel.write(cursorBuf);
    //     } catch (IOException e) {
    //         System.err.println("[debug] DiskTopic.updateCursor()");
    //         e.printStackTrace();
    //     }
    // }

    // public void clearFiles(int partition) {
    //     FileGroup fileGroup = fileGroup(partition);
    //     if (fileGroup == null) {
    //         return;
    //     }
        
    //     try {
    //         fileGroup.clearAll();
    //     } catch (IOException e) {
    //         System.err.println("[debug] DiskTopic.clearFiles() - [" + partition + "] 파티션 없음");
    //         e.printStackTrace();
    //     }
    // }

    // public Path rootPath() { return root; }
    // public Path partitionPath(int partition) { return root.resolve(String.valueOf(partition)); }
    // public SubscriberManager subscriberManager() { return manager; }

    // public record FileGroup(Path log, Path offset, Path cursor) {
    //     public FileGroup(Path root, String name) {
    //         this(root.resolve(name + ".log"), root.resolve(name + ".offset"), root.resolve(name + ".cursor"));
    //     }

    //     public void clearAll() throws IOException {
    //         Files.deleteIfExists(log);
    //         Files.deleteIfExists(offset);
    //         Files.deleteIfExists(cursor);
    //     }
    // }
}
