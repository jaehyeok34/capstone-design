package org.example.topic.disk;

import java.nio.channels.FileChannel;

import org.example.topic.TopicRecord;

import io.netty.channel.DefaultFileRegion;
import io.netty.channel.FileRegion;
import io.netty.util.ReferenceCounted;

public class DiskRecord implements TopicRecord {

    private final FileRegion region;

    public DiskRecord(FileChannel channel, long position, long count) { this(new DefaultFileRegion(channel, position, count)); }
    public DiskRecord(FileRegion region) {
        if (region == null || region.count() == 0) throw new IllegalArgumentException("region: null 또는 비어 있음");

        this.region = region;
    }


    @Override
    public int getLength() { return (int) region.count(); }
    @Override
    public ReferenceCounted getValue() { return region; }
    @Override
    public void release() { region.release(region.refCnt()); }
}
