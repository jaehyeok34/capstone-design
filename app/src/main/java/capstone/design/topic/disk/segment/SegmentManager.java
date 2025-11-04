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

import org.jspecify.annotations.Nullable;

import capstone.design.Utils;
import capstone.design.topic.TopicRecord;
import io.netty.buffer.ByteBuf;

public class SegmentManager {
    private final Path dir;
    private final String name;

    private final List<Segment> segments = new ArrayList<>();
    private final long segmentDuration; // ms
    private final Properties segmentsMetadata = new Properties();
    private final File segmentsMetadataFile;
    private Segment currentSegment;

    private final File offsetsFile;
    private final Properties offsets = new Properties();

    public SegmentManager(Path dir, String name, long segmentDuration) {
        Utils.validate(dir, name);

        this.dir = dir;
        this.name = name;

        this.segmentDuration = segmentDuration;
        this.segmentsMetadataFile = dir.resolve("segments.meta").toFile();

        this.offsetsFile = dir.resolve(name + ".offset").toFile();

        loadSegments(); // 기존 segment 로드
        loadOffsets(); // 기존 (클라이언트 논리)오프셋 로드
    }

    public int segmentCount() { return segments.size(); }

    public boolean write(ByteBuf buf) {
        long now = System.currentTimeMillis();
        if (currentSegment == null || (now - currentSegment.createdTime()) > segmentDuration) {
            rollover(now);
        }

        return currentSegment.write(buf);
    }

    @Nullable
    public TopicRecord read(String clientId, long offset) {
        /*
         * offset이 0 이상(유효값)이면 해당 오프셋을 사용하고,
         * 음수(유효하지 않은 값이면) 클라이언트의 마지막 오프셋을 사용(없으면 0)
         */
        offset = (offset >= 0) ? offset : Long.parseLong(offsets.getProperty(clientId, "0"));

        for (Segment segment : segments) {
            if (offset >= segment.baseOffset() && offset < segment.nextOffset()) {
                TopicRecord record = segment.read(offset - segment.baseOffset());
                if (record != null) {
                    updateOffsets(clientId, offset + 1);
                }

                return record;
            }
        }

        return null;
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
            // 세그먼트 파일 삭제
            for (Segment segment : segments) {
                segment.clear();
            }

            // 메타데이터, 오프셋 파일 삭제
            Files.deleteIfExists(segmentsMetadataFile.toPath());
            Files.deleteIfExists(offsetsFile.toPath());

            // 디렉토리 삭제
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            System.err.println("SegmentManager.clear(): " + e);
        }
    }

    private void loadSegments() {
        // 파일에서 세그먼트 메타데이터 로드
        try (FileInputStream in = new FileInputStream(segmentsMetadataFile)) {
            segmentsMetadata.load(in);
        } catch (Exception e) {
            System.err.println("SegmentManager.loadSegments(): " + e);
            return;
        }

        // 세그먼트 메타데이터 기반으로 세그먼트 객체 생성
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

        // 현재 세그먼트를 메타데이터에 저장
        int index = currentSegment.index();
        segmentsMetadata.setProperty("count", String.valueOf(segments.size()));
        segmentsMetadata.setProperty(index + ".baseOffset", String.valueOf(currentSegment.baseOffset()));
        segmentsMetadata.setProperty(index + ".nextOffset", String.valueOf(currentSegment.nextOffset()));
        segmentsMetadata.setProperty(index + ".createdTime", String.valueOf(currentSegment.createdTime()));

        try (FileOutputStream out = new FileOutputStream(segmentsMetadataFile)) {
            segmentsMetadata.store(out, "Segments Metadata");
        } catch (IOException e) {
            System.err.println("SegmentManager.updateSegmentsMetadata(): " + e);
        }
    }

    private void updateOffsets(String clientId, long offset) {
        offsets.setProperty(clientId, String.valueOf(offset));
        updateOffsets();
    }

    private void updateOffsets() {
        try (FileOutputStream out = new FileOutputStream(offsetsFile)) {
            offsets.store(out, "offsets");
        } catch (Exception e) {
            System.err.println("SegmentManager.updateOffsets(): " + e);
        }
    }

    public void removeOffsets(String clientId) {
        offsets.remove(clientId);
        updateOffsets();
    }

    private void loadOffsets() {
        try (FileInputStream in = new FileInputStream(offsetsFile)) {
            offsets.load(in);
        } catch (Exception e) {
            System.err.println("SegmentManager.loadOffsets(): " + e);
        }
    }

    private Path segmentFilePath(int idx, String ext) {
        return dir.resolve(name + "_" + idx + ext);
    }
}
