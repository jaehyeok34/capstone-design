package capstone.design.topic.disk.segment;

public class SegmentManager {

    // private static final String LOG_FILE_EXT = ".log";
    // private static final String IDX_FILE_EXT = ".idx";

    // private final Path directory;

    // private final List<Segment> segments = new ArrayList<>();
    // private final long duration; // 세그먼트 롤오버 기간(ms)
    // private final long retention; // 세그먼트 보관 기간(ms)
    // private final Properties metadata = new Properties();
    // private final File metaFile;
    // private Segment currentSegment;

    // public SegmentManager(Path dir, long duration, long retention) {
    //     Utils.validate(dir);

    //     this.directory = dir;

    //     this.duration = duration;
    //     this.retention = retention;
    //     this.metaFile = dir.resolve("segments.meta").toFile();

    //     this.offsetsFile = dir.resolve("offsets.properties").toFile();

    //     loadSegments(); // 기존 segment 로드
    //     loadOffsets(); // 기존 (클라이언트 논리)오프셋 로드
    // }

    // public int segmentCount() { return segments.size(); }

    // public long offset(String clientId) { return Long.parseLong(offsets.getProperty(clientId, "-1")); }

    // public boolean write(Message message) {
    //     // long now = System.currentTimeMillis();
    //     // if (currentSegment == null || (now - currentSegment.createdTime()) > duration) {
    //     //     rollover(now);
    //     // }

    //     // return currentSegment.write(buf);
    //     return true;
    // }

    // @Nullable
    // public TopicRecord read(String clientId, long offset) {
    //     /*
    //      * offset이 0 이상(유효값)이면 해당 오프셋을 사용하고,
    //      * 음수(유효하지 않은 값이면) 클라이언트의 오프셋 사용,
    //      * 그마저도 없으면 가장 오래된 세그먼트의 base offset 사용
    //      */
    //     if (offset < 0) {
    //         long clientOffset = offset(clientId);
    //         offset = (clientOffset >= 0) ? clientOffset : segments.getFirst().baseOffset();
    //     }

    //     for (Segment segment : segments) {
    //         /*
    //          * segment가 존재하더라도, 만료 됐다면(cleaner에 의해 삭제되지 않았다면)
    //          * 유효하지 않은 값으로 간주하고 건너뜀
    //          */
    //         if (segment.isExpired()) {
    //             System.err.println("SegmentManager.read(): 만료된 세그먼트: " + segment.index());
    //             continue;
    //         }

    //         if (offset >= segment.baseOffset() && offset < segment.nextOffset()) {
    //             // TopicRecord record = segment.read(offset - segment.baseOffset());
    //             // if (record != null) {
    //             //     addOffset(clientId, offset + 1);
    //             // }

    //             // return record;
    //         }
    //     }

    //     return null;
    // }

    // /**
    //  * 기존 세그먼트를 파일에 저장하고, 새로운 새그먼트 생성
    //  * 외부에서 명시적으로 새로운 파일에 기록하고자 할 때도 호출 가능
    //  */
    // public void rollover(long now) {
    //     addMetadata(); // 이전 세그먼트 메타데이터 저장

    //     int index = 0;
    //     int baseOffset = 0;

    //     if (currentSegment != null) {
    //         index = currentSegment.index() + 1;
    //         baseOffset = currentSegment.nextOffset();
    //     }

    //     Path log = directory.resolve(index + LOG_FILE_EXT);
    //     Path idx = directory.resolve(index + IDX_FILE_EXT);

    //     currentSegment = new Segment(index, log, idx, baseOffset, now, retention);
    //     segments.add(currentSegment);
    // }

    // public long messageCount() {
    //     long count = 0;
    //     for (Segment segment : segments) {
    //         count += segment.count();
    //     }

    //     return count;
    // }

    // /*
    //  * segment 내 모든 메시지가 retention 기간을 초과했는지 검사하고,
    //  * 초과한 segment를 삭제
    //  * 현재 사용 중인 segment가 삭제 대상이 될 경우 currentSegment를 null로 설정
    //  */
    // public void clean() {
    //     Iterator<Segment> it = segments.iterator();
    //     while (it.hasNext()) {
    //         Segment segment = it.next();
    //         if (segment != currentSegment && segment.isExpired()) {
    //             segment.clear(); // log, idx 파일 삭제
    //             removeMetadata(segment.index());

    //             it.remove();
    //         }
    //     }

    //     removeOffset(segments.getFirst().baseOffset());
    // }

    // public void clearAll() {
    //     try {
    //         // 세그먼트 파일 삭제
    //         for (Segment segment : segments) {
    //             segment.clear();
    //         }

    //         // 메타데이터, 오프셋 파일 삭제
    //         Files.deleteIfExists(metaFile.toPath());
    //         Files.deleteIfExists(offsetsFile.toPath());

    //         // 디렉토리 삭제
    //         Files.deleteIfExists(directory);
    //     } catch (IOException e) {
    //         System.err.println("SegmentManager.clear(): " + e);
    //     }
    // }

    // private void loadSegments() {
    //     // 파일에서 세그먼트 메타데이터 로드
    //     try (FileInputStream in = new FileInputStream(metaFile)) {
    //         metadata.load(in);
    //     } catch (Exception e) {
    //         System.err.println("SegmentManager.loadSegments(): " + e);
    //         return;
    //     }

    //     // 세그먼트 메타데이터 기반으로 세그먼트 객체 생성
    //     int count = Integer.parseInt(metadata.getProperty("count", "0"));
    //     segments.clear();

    //     /*
    //      * 로드된 metadata의 키를 순회하여 세그먼트 빌더에 반영
    //      * 1. 인덱스 추출 후 실제 log, idx 파일이 존재하는지 확인
    //      * 2. 존재하지 않는다면, 해당 인덱스는 건너뛰고 존재하면 segment builder에 반영
    //      */
    //     Map<Integer, Segment.Builder> builders = new HashMap<>();
    //     while (count-- > 0) {
    //         Iterator<Object> it = metadata.keySet().iterator();
    //         while (it.hasNext()) {
    //             String key = it.next().toString();
    //             if (key.equals("count")) {
    //                 continue;
    //             }
                
    //             int index = Integer.parseInt(key.substring(0, key.indexOf('.')));
    //             Path log = directory.resolve(index + LOG_FILE_EXT);
    //             Path idx = directory.resolve(index + IDX_FILE_EXT);
    //             if (!Files.exists(log) || !Files.exists(idx)) {
    //                 System.err.println("SegmentManager.loadSegments(): 파일이 존재하지 않음: " + log + ", " + idx);
    //                 continue;
    //             }

    //             Segment.Builder builder = builders.computeIfAbsent(index, ignored -> Segment.builder(index, log, idx));
    //             builder.keyAndValue(key, metadata.getProperty(key));
    //         }
    //     }

    //     /*
    //      * segment 생성
    //      * 인덱스 순서대로 정렬하여 segments 리스트에 추가
    //      */
    //     Object[] keys = builders.keySet().toArray();
    //     Arrays.sort(keys, Comparator.comparingInt(o -> (int) o));

    //     for (Object key :  keys) {
    //         Segment segment = builders.get(key).retention(retention).build();
    //         segments.add(segment);
    //         currentSegment = segment;
    //     }
    // }

    // /**
    //  * 세그먼트 메타데이터 업데이트 시도.
    //  * 실패 하더라도, 저장하려는 세그먼트만 유실되고 프로그램은 계속 진행됨
    //  */
    // private void addMetadata() {
    //     if (currentSegment == null) {
    //         return;
    //     }

    //     // 현재 세그먼트를 메타데이터에 저장
    //     int index = currentSegment.index();
    //     metadata.setProperty("count", String.valueOf(segments.size()));
    //     metadata.setProperty(index + ".baseOffset", String.valueOf(currentSegment.baseOffset()));
    //     metadata.setProperty(index + ".nextOffset", String.valueOf(currentSegment.nextOffset()));
    //     metadata.setProperty(index + ".createdTime", String.valueOf(currentSegment.createdTime()));

    //     updateMetadata();
    // }

    // private void removeMetadata(int index) {
    //     boolean removed = false;
    //     Iterator<Object> it = metadata.keySet().iterator();
    //     while (it.hasNext()) {
    //         if (it.next().toString().startsWith(index + ".")) {
    //             it.remove();
    //             removed = true;
    //         }
    //     }

    //     if (removed) {
    //         int count = Integer.parseInt(metadata.getProperty("count"));
    //         metadata.setProperty("count", String.valueOf(count - 1));
    //     }

    //     updateMetadata();
    // }

    // private void updateMetadata() {
    //     try (FileOutputStream out = new FileOutputStream(metaFile)) {
    //         metadata.store(out, "Segments Metadata");
    //     } catch (IOException e) {
    //         System.err.println("SegmentManager.updateSegmentsMetadata(): " + e);
    //     }
    // }

    // /**
    //  * properties 객체에 우선 반영 후 파일에 저장
    //  */
    // private void addOffset(String clientId, long offset) {
    //     offsets.setProperty(clientId, String.valueOf(offset));
    //     updateOffsets();
    // }

    // /**
    //  * 모든 offset 정보 중에서 pivot보다 작은 offset을 갖는 항목 삭제
    //  */
    // private void removeOffset(long pivot) {
    //     Iterator<Object> it = offsets.keySet().iterator();
    //     while (it.hasNext()) {
    //         String clientId = it.next().toString();
    //         long offset = Long.parseLong(offsets.getProperty(clientId));
    //         if (offset < pivot) {
    //             it.remove();
    //         }
    //     }
    //     updateOffsets();
    // }

    // /**
    //  * 실제 offsets.properties 파일에 반영
    //  */
    // private void updateOffsets() {
    //     try (FileOutputStream out = new FileOutputStream(offsetsFile)) {
    //         offsets.store(out, "offsets");
    //     } catch (Exception e) {
    //         System.err.println("SegmentManager.updateOffsets(): " + e);
    //     }
    // }

    // public void removeOffsets(String clientId) {
    //     offsets.remove(clientId);
    //     updateOffsets();
    // }

    // private void loadOffsets() {
    //     try (FileInputStream in = new FileInputStream(offsetsFile)) {
    //         offsets.load(in);
    //     } catch (Exception e) {
    //         System.err.println("SegmentManager.loadOffsets(): " + e);
    //     }
    // }
}
