package org.example.topic;

import org.example.message.Message;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.example.message.MessageProcessor;
import org.example.topic.disk.DiskTopic;
import org.example.topic.memory.MemoryTopic;

public class TopicManager implements MessageProcessor {

    private final Map<String, Topic> topicTable = new ConcurrentHashMap<>();

    public TopicManager(Map<String, Topic.Type> topicInfo) {
        if (topicInfo == null) throw new IllegalArgumentException("topicInfo: null");

        topicInfo.forEach((name, type) -> {
            if (name == null || name.isEmpty() || type == null) return;

            try {
                Topic topic = type == Topic.Type.MEMORY ? MemoryTopic.of() : DiskTopic.of(name);
                topicTable.put(name, topic);
            } catch (IOException ignore) { return; }
        });
    }

    public Optional<Topic> topic(String name) { return Optional.ofNullable(topicTable.get(name)); }

    @Override
    public Result process(MessageFrame frame) {
        MessageHeader header = frame.header();
        Message message = frame.message();

        switch (header.type()) {
            case MessageHeader.Type.REQ_PUSH -> { 
                System.out.println("[debug] TopicManager.process() - REQ_PUSH 처리...");
                return push(header, message);
            }

            case MessageHeader.Type.REQ_PULL -> {
                System.out.println("[debug] TopicManager.process() - REQ_PULL 처리...");
                return pull(header);
            }

            default -> {
                System.out.println("[debug] TopicManager.process() - RES_XXX 무시");
                throw new IllegalStateException("type: RES_XXX");
            }
        }
    }

    private Result pull(MessageHeader header) {
        String topicName = header.topicName();
        int partition = header.partition();

        MessageHeader.Builder builder = MessageHeader
            .builder(MessageHeader.Type.RES_PULL, topicName)
            .partition(partition);

        return Optional.ofNullable(topicTable.get(topicName))
            .flatMap(topic -> topic.pull(partition)) // Optional<TopicRecord>
            .map(record -> {
                MessageHeader resHeader = builder
                    .messageLength(record.length())
                    .build();

                return Result.of(resHeader, record);
            }).orElse(Result.of(builder.build())); // no topic or no record
    }

    private Result push(MessageHeader header, Message message) {
        Optional.ofNullable(topicTable.get(header.topicName()))
            .ifPresent(topic -> topic.push(header.partition(), message.retain()));

        MessageHeader resHeader = MessageHeader
            .builder(MessageHeader.Type.RES_PUSH, header.topicName())
            .partition(header.partition())
            .build();

        return Result.of(resHeader);
    }
}
