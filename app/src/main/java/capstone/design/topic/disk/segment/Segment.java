package capstone.design.topic.disk.segment;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import capstone.design.Utils;
import io.netty.buffer.ByteBuf;

public class Segment {
    
    private final int index;
    private final Path log;
    private final Path idx;
    private final int baseOffset;
    private int nextOffset;
    private final long createdTime;

    public Segment(int index, Path log, Path idx, int baseOffset, int nextOffset, long createdTime) {
        Utils.validate(log, idx);

        this.index = index;
        this.log = log;
        this.idx = idx;
        this.baseOffset = baseOffset;
        this.nextOffset = nextOffset;
        this.createdTime = createdTime;
    }

    public Segment(int index, Path log, Path idx, int baseOffset, long createdTime) {
        Utils.validate(log, idx);

        this.index = index;
        this.log = log;
        this.idx = idx;
        this.baseOffset = baseOffset;
        this.nextOffset = getNextOffset();
        this.createdTime = createdTime;
    }

    public int index() { return index; }
    public Path log() { return log; }
    public Path idx() { return idx; }
    public int baseOffset() { return baseOffset; }
    public int nextOffset() { return nextOffset; }
    public long createdTime() { return createdTime; }
    public int count() { return nextOffset - baseOffset; }

    public boolean write(ByteBuf buf) {
        OpenOption[] options = new OpenOption[] {
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND
        };

        try (
            FileChannel logFile = FileChannel.open(log, options);
            FileChannel idxFile = FileChannel.open(idx, options);
        ) {
            long offset = writeLog(logFile, buf);
            writeIndex(idxFile, offset);
            nextOffset++;

            return true;
        } catch (Exception e) {
            System.err.println("세그먼트 파일 쓰기 오류: " + e);
            return false; 
        }
    }

    public void read() {

    }

    public void clear() throws IOException {
        Files.deleteIfExists(log);
        Files.deleteIfExists(idx);
    }

    private long writeLog(FileChannel file, ByteBuf buf) throws IOException {
        // 다음 쓰기 위치 획득
        long offset = file.position();

        // 메시지 길이 기록
        ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
        lengthBuf.putInt(buf.readableBytes()).flip();
        file.write(lengthBuf);  

        // 메시지 내용 기록
        file.write(buf.nioBuffer());

        return offset;
    }

    private void writeIndex(FileChannel file, long offset) throws IOException {
        ByteBuffer offsetBuf = ByteBuffer.allocate(Long.BYTES);
        offsetBuf.putLong(offset).flip();
        file.write(offsetBuf);
    }

    /**
     * idx 파일의 크기로부터 nextOffset 계산하여 반환
     * idx 파일이 존재하지 않을경우 File.length()는 0을 반환(예외 X)
     */
    private int getNextOffset() {
        return (int) (new File(idx.toString()).length() / Long.BYTES + baseOffset);
    }
}
