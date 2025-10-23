package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.example.topic.TopicRecord;
import org.example.topic.disk.DiskTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;

public class DiskTopicTest {

    List<DiskTopic> topics = new ArrayList<>();
    int topicCount, partitionCount;
    final String topicName = "test";
    final String message = "message";

    void addData(DiskTopic topic, int partition, String s) {
        topic.push(partition, Unpooled.copiedBuffer(message + s, Charset.defaultCharset()));
    }

    @BeforeEach
    void beforeEach() throws IOException {
        topicCount = new Random().nextInt(3) + 1;
        partitionCount = new Random().nextInt(5) + 1;

        for (int i = 0; i < topicCount; i++) {
            topics.add(DiskTopic.of(topicName + i));
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
    }

    @Test
    void push() {
        System.out.println("topic count: " + topicCount);
        System.out.println("partition count: " + partitionCount);

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
    void pull() throws IOException {
        System.out.println("topic count: " + topicCount);
        System.out.println("partition count: " + partitionCount);

        // 데이터 준비
        topics.forEach(topic -> {
            for (int i = 0; i < partitionCount; i++) {
                int n = new Random().nextInt(10) + 1;
                for (int j = 0; j < n; j++) {
                    addData(topic, i, String.valueOf(j));
                }
            }
        });

        // 데이터 검증
        topics.forEach(topic -> {
            for (int i = 0; i < partitionCount; i++) {
                for (int j = 0; j < topic.length(i); j++) {
                    TopicRecord record = topic.pull(i).get();
                    if (record.value() instanceof FileRegion region) {
                        try (
                            OutputStream out = new ByteArrayOutputStream();
                            WritableByteChannel channel = Channels.newChannel(out);
                        ) {
                            region.transferTo(channel, 0);
                            assertEquals(message + j, out.toString());
                            assertEquals(j + 1, topic.cursor(i));
                        } catch (IOException ignore) {}
                    }
                }
            }
        });
    }
}
