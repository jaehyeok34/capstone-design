package capstone.design;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import capstone.design.spy.SpyContext;
import capstone.design.topic.TopicRecord;
import capstone.design.topic.disk.DiskTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;

public class DiskTopicTest {

    List<DiskTopic> topics = new ArrayList<>();
    int topicCount, partitionCount;
    final String topicName = "test";
    final String message = "message";
    final List<ByteBuf> bufs = new ArrayList<>();

    void addData(DiskTopic topic, int partition, String msg) {
        ByteBuf buf = Unpooled.copiedBuffer((message + msg).getBytes(StandardCharsets.UTF_8));
        bufs.add(buf);
        topic.push(partition, buf);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        topicCount = new Random().nextInt(3) + 1;
        partitionCount = new Random().nextInt(5) + 1;

        for (int i = 0; i < topicCount; i++) {
            topics.add(new DiskTopic(topicName + i));
        }
    }

    @AfterEach
    void afterEach() throws IOException {
        topics.forEach(topic -> {
            // clear partition files
            for (int i = 0; i < partitionCount; i++) {
                topic.clearFiles(i);
                try {
                    Files.deleteIfExists(topic.partitionPath(i));
                } catch (Exception ignore) {}
            }
            
            // clear topic
            try {
                Files.deleteIfExists(topic.rootPath());
            } catch (Exception ignore) {}
            topic = null;
        });

        topics = null;
        bufs.forEach(buf -> { if(buf.refCnt() > 0) buf.release(); });
    }

    @Test
    void pushTest() {
        topics.forEach(topic -> {
            for (int i = 0; i < partitionCount; i++) {
                int n = new Random().nextInt(10) + 1;
                for (int j = 0; j < n; j++) {
                    addData(topic, i, String.valueOf(j));
                }

                assertEquals(n, topic.length(i));
            }
        });
    }

    @Test
    void pullTest() throws Exception {
        // 데이터 준비
        DiskTopic topic = topics.getFirst();
        
        for (int i = 0; i < 3; i++) {
            addData(topic, 0, String.valueOf(i));
        }

        // pull
        for (int i = 0; i < 2; i++) { // 동일 조건으로 동일 커서 2회 검증
            TopicRecord record = topic.pull(0, 0) instanceof TopicRecord r ? r : null;
            if (record.value() instanceof FileRegion region) {
                try (
                    OutputStream out = new ByteArrayOutputStream();
                    WritableByteChannel channel = Channels.newChannel(out);
                ) {
                    region.transferTo(channel, 0);
    
                    // cursor: 0을 읽었을 때 검증
                    assertEquals(message + 0, out.toString());
                    assertEquals(1, topic.cursor(0));
                } catch (IOException ignore) {}
            }
        }

        // 다음 cursor 읽기 검증
        TopicRecord record = topic.pull(0) instanceof TopicRecord r ? r : null;
        if (record.value() instanceof FileRegion region) {
            try (
                OutputStream out = new ByteArrayOutputStream();
                WritableByteChannel channel = Channels.newChannel(out);
            ) {
                region.transferTo(channel, 0);
                assertEquals(message + 1, out.toString());
                assertEquals(2, topic.cursor(0 ));
            } catch (IOException ignore) {}
        }
    }
    
    @Test
    void subscribeTest() {
        SpyContext context = new SpyContext();
        DiskTopic topic = topics.getFirst();
        int partition = 0;
        
        assertEquals(0, topic.subscriberManager().count(partition));

        topic.subscribe(context, partition, "s1");
        assertEquals(1, topic.subscriberManager().count(partition));

        assertDoesNotThrow(() -> topic.unsubscribe(partition, "s0"));

        topic.unsubscribe(partition, "s1");
        assertEquals(0, topic.subscriberManager().count(partition));
    }
}
