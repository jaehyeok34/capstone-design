package capstone.design.topic.disk;

import java.nio.channels.FileChannel;

import capstone.design.topic.TopicRecord;

import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.util.ReferenceCounted;

public class DiskRecord implements TopicRecord {

    private final FileRegion region;

    private DiskRecord(FileRegion region) {
        if (region == null || region.count() == 0) {
            throw new IllegalArgumentException("region: null 또는 비어 있음");
        }

        this.region = region;
    }

    public static DiskRecord of(FileRegion region) { return new DiskRecord(region); }
    public static DiskRecord of(FileChannel channel, long position, long count) {
        FileRegion region = new DefaultFileRegion(channel, position, count);
        return new DiskRecord(region);
    }

    @Override
    public int length() { return (int) region.count(); }
    @Override
    public ReferenceCounted value() { return region; }
    @Override
    public void release() { region.release(region.refCnt()); }
}
