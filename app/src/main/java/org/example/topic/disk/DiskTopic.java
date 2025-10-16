package org.example.topic.disk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.example.topic.TopicRecord;
import org.example.topic.Topic;

import io.netty.buffer.ByteBuf;

public class DiskTopic implements Topic {

    private final FileGroup fileGroup;

    public DiskTopic(String name) throws IOException {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("name: null 또는 빈 문자열");

        Path root = Files.createDirectories(Paths.get("topic", name));
        this.fileGroup = new FileGroup(root, name);
    }

    public FileGroup getFileGroup() { return fileGroup; }

    @Override
    public long getLength() { return fileGroup.offset().toFile().length() / Long.BYTES; }

    @Override
    public TopicRecord pull() {
        try (
            RandomAccessFile offsetFile = new RandomAccessFile(fileGroup.offset().toFile(), "r");
        ) {
            // cursor 획득
            long cursor = readCursor();
            if (cursor >= getLength()) {
                System.out.println("[debug] 더이상 읽을 메시지 없음");
                return null;
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
            DiskRecord record = new DiskRecord(logFile, offset + Integer.BYTES, length);

            // cursor 갱신
            updateCursor(cursor + 1);

            return record;
        } catch (Exception e) { e.printStackTrace(); }

        return null;
    }

    @Override
    public void push(ByteBuf buf) {
        if (buf == null || buf.readableBytes() == 0) throw new IllegalArgumentException("buf: null 또는 빈 버퍼");

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
            buf.release();
        }
    }

    private long writeLog(ByteBuf buf, FileChannel fc) throws IOException {
        // 메시지 offset 획득
        long offset = fc.position();

        // 메시지 길이 기록
        ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
        lengthBuf.putInt(buf.readableBytes());
        lengthBuf.flip();
        fc.write(lengthBuf);

        // 메시지 내용 기록
        fc.write(buf.nioBuffer());

        return offset;
    }

    private void writeOffset(long offset, FileChannel fc) throws IOException {
        ByteBuffer offsetBuf = ByteBuffer.allocate(Long.BYTES);
        offsetBuf.putLong(offset);
        offsetBuf.flip();
        fc.write(offsetBuf);
    }

    private long readCursor() {
        try (
            FileChannel cursorFileChannel = FileChannel.open(fileGroup.cursor(),
                StandardOpenOption.CREATE, StandardOpenOption.READ);
        ) {
            ByteBuffer cursorBuf = ByteBuffer.allocate(Long.BYTES);
            int read = cursorFileChannel.read(cursorBuf);
            if (read != Long.BYTES) {
                return 0L;
            }

            cursorBuf.flip();
            return cursorBuf.getLong();
        } catch (IOException e) {
            System.out.println("[debug] readCursor() - 문제 발생");
            e.printStackTrace();
            return 0L;
        }
    }

    private void updateCursor(long cursor) {
        try (
            FileChannel cursorFileChannel = FileChannel.open(fileGroup.cursor(),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        ) {
            ByteBuffer cursorBuf = ByteBuffer.allocate(Long.BYTES);
            cursorBuf.putLong(cursor);
            cursorBuf.flip();
            cursorFileChannel.write(cursorBuf);
        } catch (IOException e) {
            System.out.println("[debug] updateCursor() - 문제 발생");
            e.printStackTrace();
        }
    }

    public long getCursor() throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(Files.readAllBytes(fileGroup.cursor()));
        return buffer.getLong();
    }

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
