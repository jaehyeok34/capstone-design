package capstone.design.topic.disk.segment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import capstone.design.Utils;
import io.netty.buffer.ByteBuf;

public class SegmentManager {
    private final Path dir;
    private final String name;
    private final long segmentDuration; // ms
    private final List<Segment> segments = new ArrayList<>();
    private Properties segmentsMetadata;
    private final File segmentsMetadataFile;

    private Segment currentSegment;

    public SegmentManager(Path dir, String name, long segmentDuration) {
        Utils.validate(dir, name);

        this.dir = dir;
        this.name = name;
        this.segmentDuration = segmentDuration;
        this.segmentsMetadataFile = new File(dir.resolve("segments.meta").toString());

        loadSegments();
    }

    public int segmentCount() { return segments.size(); }

    public boolean write(ByteBuf buf) {
        long now = System.currentTimeMillis();
        if (currentSegment == null || (now - currentSegment.createdTime()) > segmentDuration) {
            rollover(now);
        }

        return currentSegment.write(buf);
    }

    /**
     * 기존 세그먼트를 파일에 저장하고, 새로운 새그먼트 생성
     * 외부에서 명시적으로 새로운 파일에 기록하고자 할 때도 호출 가능
     */
    public void rollover(long now) {
        updateSegmentsMetadata(); // 이전 세그먼트 메타데이터 저장

        int index = segments.size();
        Path log = segmentFilePath(index, ".log");
        Path idx = segmentFilePath(index, ".idx");
        int baseOffset = (currentSegment != null) ? currentSegment.nextOffset() : 0;

        currentSegment = new Segment(index, log, idx, baseOffset, now);
        segments.add(currentSegment);
    }

    public long messageCount() {
        long count = 0;
        for (Segment segment : segments) {
            count += segment.count();
        }

        return count;
    }

    public void clear() {
        try {
            // 파일 삭제
            for (Segment segment : segments) {
                segment.clear();
            }

            // 디렉토리 삭제
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            System.err.println("SegmentManager.clear(): " + e);
        }
    }

    private void loadSegments() {
        segmentsMetadata = new Properties();
        try (FileInputStream in = new FileInputStream(segmentsMetadataFile)) {
            segmentsMetadata.load(in);
        } catch (Exception e) {
            System.err.println("SegmentManager.loadSegments(): " + e);
            return;
        }

        int segmentCount = Integer.parseInt(segmentsMetadata.getProperty("count", "0"));
        segments.clear();

        for (int i = 0; i < segmentCount; i++) {
            int baseOffset = Integer.parseInt(segmentsMetadata.getProperty(i + ".baseOffset", "0"));
            int nextOffset = Integer.parseInt(segmentsMetadata.getProperty(i + ".nextOffset", "0"));
            long createdTime = Long.parseLong(segmentsMetadata.getProperty(i + ".createdTime", "0"));

            Path log = segmentFilePath(i, ".log");
            Path idx = segmentFilePath(i, ".idx");

            if (!Files.exists(log) || !Files.exists(idx)) {
                System.err.println("SegmentManager.loadSegments(): 파일이 존재하지 않음: " + log + ", " + idx);
                continue;
            }

            Segment segment = new Segment(i, log, idx, baseOffset, nextOffset, createdTime);
            segments.add(segment);
            currentSegment = segment;
        }
    }

    /**
     * 세그먼트 메타데이터 업데이트 시도.
     * 실패 하더라도, 저장하려는 세그먼트만 유실되고 프로그램은 계속 진행됨
     */
    private void updateSegmentsMetadata() {
        if (currentSegment == null) {
            return;
        }

        int index = currentSegment.index();
        Properties props = (segmentsMetadata != null) ? segmentsMetadata : new Properties();
        props.setProperty("count", String.valueOf(segments.size()));
        props.setProperty(index + ".baseOffset", String.valueOf(currentSegment.baseOffset()));
        props.setProperty(index + ".nextOffset", String.valueOf(currentSegment.nextOffset()));
        props.setProperty(index + ".createdTime", String.valueOf(currentSegment.createdTime()));

        try (FileOutputStream out = new FileOutputStream(segmentsMetadataFile)) {
            props.store(out, "Segments Metadata");
        } catch (IOException e) {
            System.err.println("SegmentManager.updateSegmentsMetadata(): " + e);
        }
    }

    private Path segmentFilePath(int idx, String ext) {
        return dir.resolve(name + "_" + idx + ext);
    }
}
