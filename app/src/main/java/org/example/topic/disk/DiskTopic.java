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
import org.example.topic.Topic;

import io.netty.buffer.ByteBuf;

public class DiskTopic implements Topic {

    private final String name;
    private final Path root;

    private DiskTopic(String name) throws IOException {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name: null 또는 빈 문자열");
        }
        
        this.name = name;
        this.root = Files.createDirectories(Paths.get("topic", name));
    }

    public static DiskTopic of(String name) throws IOException {
        return new DiskTopic(name);
    }

    public static Optional<DiskTopic> ofNullable(String name) {
        try {
            return Optional.of(new DiskTopic(name));
        } catch (Exception e) {
            System.err.println("[debug] DiskTopic.ofNullable() - Exception");
            return Optional.empty();
        }
    }

    @Override
    public long length(int partition) { 
        return fileGroup(partition)
            .map(fileGroup -> fileGroup.offset().toFile().length() / Long.BYTES)
            .orElse(0L);
    }

    @Override
    public Optional<TopicRecord> pull(int partition) {
        FileGroup fileGroup = fileGroup(partition).orElse(null);
        if (fileGroup == null) {
            System.err.println("[debug] DiskTopic.pull() - [" + partition + "] 파티션 없음");
            return Optional.empty();
        }

        try (
            RandomAccessFile offsetFile = new RandomAccessFile(fileGroup.offset().toFile(), "r");
        ) {
            // cursor 획득
            long cursor = readCursor(fileGroup);
            if (cursor >= length(partition)) {
                throw new IllegalStateException("읽을 메시지 없음");
            }
            
            // offset 획득
            offsetFile.seek(cursor * Long.BYTES);
            long offset = offsetFile.readLong();
            
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

            return Optional.of(record);
        } catch (Exception e) { 
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /*
     * ByteBuf buf는 push가 끝나면 모든 refCnt를 감소시킴
     * 디스크에 이미 저장을 했기 때문에 메모리에는 남아있을 필요가 없음
     */
    @Override
    public void push(int partition, ByteBuf buf) {
        if (buf == null || buf.readableBytes() == 0 || buf.refCnt() < 1) {
            throw new IllegalArgumentException("buf: null or empty or released");
        }

        Path path;
        try {
            path = Files.createDirectories(root.resolve(String.valueOf(partition)));
        } catch (IOException e) {
            System.err.println("[debug] DiskTopic.push() - IOException");
            return;
        }

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
        }
    }
    
    private Optional<FileGroup> fileGroup(int partition) {
        Path path = root.resolve(String.valueOf(partition));
        return Files.exists(path) ? 
            Optional.of(new FileGroup(path, name)) : 
            Optional.empty();
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
        } catch (IOException e) {
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
        return fileGroup(partition)
            .map(this::readCursor)
            .orElse(0L);
    }

    public void clearFiles(int partition) {
        fileGroup(partition)
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
