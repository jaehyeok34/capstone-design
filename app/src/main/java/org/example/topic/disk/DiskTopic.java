package org.example.topic.disk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.example.topic.TopicRecord;
import org.example.topic.subscribe.SubscriberManager;
import org.jspecify.annotations.Nullable;
import org.example.Utils;
import org.example.topic.Topic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class DiskTopic implements Topic {

    private final String name;
    private final Path root;
    private final SubscriberManager manager;

    public DiskTopic(String name) throws IOException {
        Utils.validate(name);        

        this.name = name;
        this.root = Files.createDirectories(Paths.get("topic", name));
        this.manager = new SubscriberManager(name);
    }

    /*
     * ByteBuf buf는 push가 끝나면 모든 refCnt를 감소시킴
     * 디스크에 이미 저장을 했기 때문에 메모리에는 남아있을 필요가 없음
     */
    @Override
    public void push(int partition, String id, ByteBuf buf) {
        Utils.validate(buf);

        // 파티션 디렉토리 생성(이미 존재한다면 무시)
        Path path;
        try {
            path = Files.createDirectories(root.resolve(String.valueOf(partition)));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // 메시지 기록
        FileGroup fileGroup = new FileGroup(path, name);
        try (
            FileChannel logFileChannel = FileChannel.open(fileGroup.log(), 
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            FileChannel offsetFileChannel = FileChannel.open(fileGroup.offset(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        ) {
            long offset = writeLog(buf, logFileChannel);
            writeOffset(offset, offsetFileChannel);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            buf.release(buf.refCnt());
            manager.notify(partition);
        }
    }
    public void push(int partition, ByteBuf buf) { push(partition, null, buf); }
    
    @Override
    public TopicRecord pull(int partition, String id, Long cursor) {
        FileGroup fileGroup = fileGroup(partition);
        if (fileGroup == null) {
            System.err.println("[debug] DiskTopic.pull() - [" + partition + "] 파티션 없음");
            return null;
        }
        
        try (
            RandomAccessFile offsetFile = new RandomAccessFile(fileGroup.offset().toFile(), "r");
        ) {
            // cursor 획득
            if (cursor == null || cursor < 0) {
                cursor = readCursor(fileGroup);
            }
            
            // offset 획득
            offsetFile.seek(cursor * Long.BYTES);
            Long offset = offsetFile.readLong();

            // message 획득
            FileChannel logFile = FileChannel.open(fileGroup.log(), StandardOpenOption.READ);
            ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
            logFile.read(lengthBuf, offset);
            lengthBuf.flip();
            int length = lengthBuf.getInt();
            
            // DiskRecord 생성
            // offset + Integer.BYTES: 메시지 길이 정보는 읽었으니 건너 뛰고 실제 메시지부터 읽기 위함
            DiskRecord record = DiskRecord.of(logFile, offset + Integer.BYTES, length);
            
            // cursor 갱신
            updateCursor(fileGroup, cursor + 1);
            
            return record;
        } catch (Exception e) { 
            e.printStackTrace();
            return null;
        }
    }
    public TopicRecord pull(int partition, long cursor) { return pull(partition, null, cursor); }
    public TopicRecord pull(int partition) { return pull(partition, null, null); }
        
    @Override
    public void subscribe(ChannelHandlerContext context, int partition, String id) {
        manager.subscribe(context, partition, id);
    }

    @Override
    public void unsubscribe(int partition, String id) {
        manager.unsubscribe(partition, id);
    }

    
    @Override
    public long length(int partition, String id) { 
        /*
        * id는 memory topic과 동일한 메서드 시그니처 유지를 위해 남겨둠
        * 실제로는 사용하지 않음
        */
        return Optional.ofNullable(fileGroup(partition))
        .map(group -> group.offset().toFile().length() / Long.BYTES)
        .orElse(0L);
    }
    public long length(int partition) { return length(partition, null); }
    
    @Nullable
    private FileGroup fileGroup(int partition) {
        Path path = root.resolve(String.valueOf(partition));
        if (Files.notExists(path)) {
            return null;
        }

        return new FileGroup(path, name);
    }

    private long writeLog(ByteBuf buf, FileChannel channel) throws IOException {
        // 메시지 offset 획득
        long offset = channel.position();

        // 메시지 길이 기록
        ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
        lengthBuf.putInt(buf.readableBytes());
        lengthBuf.flip();
        channel.write(lengthBuf);

        // 메시지 내용 기록
        channel.write(buf.nioBuffer());

        return offset;
    }

    private void writeOffset(long offset, FileChannel channel) throws IOException {
        ByteBuffer offsetBuf = ByteBuffer.allocate(Long.BYTES);
        offsetBuf.putLong(offset);
        offsetBuf.flip();
        channel.write(offsetBuf);
    }

    private long readCursor(FileGroup fileGroup) {
        try (
            FileChannel cursorFileChannel = FileChannel.open(fileGroup.cursor(),
                StandardOpenOption.CREATE, StandardOpenOption.READ);
        ) {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            int read = cursorFileChannel.read(buffer);
            if (read != Long.BYTES) {
                return 0; // 커서 파일이 비어있으면 0으로 간주
            }

            buffer.flip();
            return buffer.getLong();
        } catch (IOException ignored) {
            System.err.println("[debug] DiskTopic.readCursor() - IOException");
            return 0;
        }
    }

    private void updateCursor(FileGroup fileGroup, long cursor) {
        try (
            FileChannel cursorFileChannel = FileChannel.open(fileGroup.cursor(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        ) {
            ByteBuffer cursorBuf = ByteBuffer.allocate(Long.BYTES);
            cursorBuf.putLong(cursor);
            cursorBuf.flip();
            cursorFileChannel.write(cursorBuf);
        } catch (IOException e) {
            System.err.println("[debug] DiskTopic.updateCursor() - IOException");
            e.printStackTrace();
        }
    }

    public long cursor(int partition) {
        return Optional.ofNullable(fileGroup(partition))
            .map(this::readCursor)
            .orElse(0L);
    }

    public void clearFiles(int partition) {
        Optional.ofNullable(fileGroup(partition))
            .ifPresent(fileGroup -> {
                try {
                    fileGroup.clearAll();
                } catch (Exception e) {
                    System.err.println("[debug] DiskTopic.clearFiles() - [" + partition + "] 파일 삭제 실패");
                }
            });
    }

    public Path rootPath() { return root; }
    public Path partitionPath(int partition) { return root.resolve(String.valueOf(partition)); }
    public SubscriberManager subscriberManager() { return manager; }

    public record FileGroup(Path log, Path offset, Path cursor) {
        public FileGroup(Path root, String name) {
            this(root.resolve(name + ".log"), root.resolve(name + ".offset"), root.resolve(name + ".cursor"));
        }

        public void clearAll() throws IOException {
            Files.deleteIfExists(log);
            Files.deleteIfExists(offset);
            Files.deleteIfExists(cursor);
        }
    }
}
