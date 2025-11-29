package capstone.design.topic.disk.segment;

import java.nio.file.Path;

public class Segment {

    private final Path log;
    private final Path idx;
    private final int startOffset;
    private int endOffset;
    private final long createdAt;

    private Segment(Path log, Path idx, int startOffset, int endOffset, long createdAt) {
        this.log = log;
        this.idx = idx;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.createdAt = createdAt;
    }

    public static Segment of(Path log, Path idx, int startOffset, long createdAt) {
        return new Segment(log, idx, startOffset, startOffset, createdAt);
    }

    public static Segment of(Path log, Path idx, int startOffset, int endOffset, long createdAt) {
        return new Segment(log, idx, startOffset, endOffset, createdAt);
    }   

    public Path log() { return log; }
    public Path idx() { return idx; }
    public int startOffset() { return startOffset; } 
    public int endOffset() { return endOffset; }
    public int count() { return endOffset - startOffset; }
    public boolean isExpired(long retention) { return (System.currentTimeMillis() - createdAt) > retention; }

    // public boolean write(byte[] buf) {
    //     OpenOption[] options = new OpenOption[] {
    //         StandardOpenOption.CREATE,
    //         StandardOpenOption.WRITE,
    //         StandardOpenOption.APPEND
    //     };

    //     try (
    //         FileChannel logFile = FileChannel.open(log, options);
    //         FileChannel idxFile = FileChannel.open(idx, options);
    //     ) {
    //         long pos = writeLog(logFile, buf);
    //         writeIdx(idxFile, pos);
    //         nextOffset++;

    //         return true;
    //     } catch (Exception e) {
    //         System.err.println("세그먼트 파일 쓰기 오류: " + e);
    //         return false; 
    //     }
    // }

    // public TopicRecord read(long offset) {
    //     try (RandomAccessFile idxFile = new RandomAccessFile(idx.toFile(), "r")) {
    //         // idx 파일에서 메시지 실제 위치 획득
    //         idxFile.seek(offset * Long.BYTES);
    //         long position = idxFile.readLong();

    //         // 실제 위치 기반 메시지 읽기(길이 먼저)
    //         FileChannel logFile = FileChannel.open(log, StandardOpenOption.READ);
    //         ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
    //         logFile.read(lengthBuf, position);
    //         lengthBuf.flip();
    //         int length = lengthBuf.getInt();
            
    //         // return DiskRecord.of(logFile, position + Integer.BYTES, length);
    //         return null;
    //     } catch (Exception e) {
    //         System.err.println("Segment.read(): " + e);
    //         return null;
    //     }
    // }

    // public void clear() {
    //     try {
    //         Files.deleteIfExists(log);
    //         Files.deleteIfExists(idx);
    //     } catch (IOException e) {
    //         System.err.println("Segment.clear(): " + e);
    //     }
    // }

    // private long writeLog(FileChannel file, byte[] buf) throws IOException {
    //     // 다음 쓰기 위치 획득
    //     long pos = file.position();

    //     // 메시지 길이 기록
    //     ByteBuffer lengthBuf = ByteBuffer.allocate(Integer.BYTES);
    //     lengthBuf.putInt(buf.length).flip();
    //     file.write(lengthBuf);  

    //     // 메시지 내용 기록
    //     file.write(ByteBuffer.wrap(buf));

    //     return pos;
    // }

    // private void writeIdx(FileChannel file, long position) throws IOException {
    //     ByteBuffer offsetBuf = ByteBuffer.allocate(Long.BYTES);
    //     offsetBuf.putLong(position).flip();
    //     file.write(offsetBuf);
    // }

    // /**
    //  * idx 파일의 크기로부터 nextOffset 계산하여 반환
    //  * idx 파일이 존재하지 않을경우 File.length()는 0을 반환(예외 X)
    //  */
    // private int getNextOffset() {
    //     return (int) (new File(idx.toString()).length() / Long.BYTES + baseOffset);
    // }

    // public static class Builder {
        
    //     private final Path log;
    //     private final Path idx;
    //     private final int index;

    //     private Integer baseOffset = null;
    //     private Integer nextOffset = null;
    //     private Long createdTime = null;
    //     private Long retention = null;

    //     public Builder(int index, Path log, Path idx) {
    //         Utils.validate(log, idx);

    //         this.index = index;
    //         this.log = log;
    //         this.idx = idx;
    //     }

    //     public Segment build() {
    //         Utils.validate(index, baseOffset, createdTime, retention);

    //         return switch (nextOffset) {
    //             case null -> new Segment(index, log, idx, baseOffset, createdTime, retention);
    //             default -> new Segment(index, log, idx, baseOffset, nextOffset, createdTime, retention);
    //         };
    //     }

    //     public Builder baseOffset(int baseOffset) {
    //         this.baseOffset = baseOffset;
    //         return this;
    //     }

    //     public Builder nextOffset(int nextOffset) {
    //         this.nextOffset = nextOffset;
    //         return this;
    //     }

    //     public Builder createdTime(long createdTime) {
    //         this.createdTime = createdTime;
    //         return this;
    //     }

    //     public Builder retention(long retention) {
    //         this.retention = retention;
    //         return this;
    //     }

    //     public Builder keyAndValue(String key, String value) {
    //         if (key.contains("baseOffset")) {
    //             this.baseOffset = Integer.parseInt(value);
    //         } else if (key.contains("nextOffset")) {
    //             this.nextOffset = Integer.parseInt(value);
    //         } else if (key.contains("createdTime")) {
    //             this.createdTime = Long.parseLong(value);
    //         }

    //         return this;
    //     }
    // }
}
