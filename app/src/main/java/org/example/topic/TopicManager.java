package org.example.topic;

import org.example.message.Message;
import org.example.message.MessageFrame;
import org.example.message.MessageHeader;

import java.io.IOException;
import java.util.Map;
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
                Topic topic = type == Topic.Type.MEMORY ? new MemoryTopic() : new DiskTopic(name);
                topicTable.put(name, topic);
            } catch (IOException ignore) {}
        });
    }

    public Topic getTopic(String name) { return topicTable.get(name); }

    @Override
    public Result process(MessageFrame frame) {
        if (frame == null) throw new IllegalArgumentException("frame: null");

        MessageHeader header = frame.header();
        Message message = frame.message();

        // REQ_PUSH와 REQ_PULL 처리
        if (header.getType() == MessageHeader.Type.REQ_PULL) {
            System.out.println("[debug] TopicManager.process() - REQ_PULL 처리...");
            TopicRecord record = topicTable.get(message.getTopicName()).pull();

            MessageHeader resHeader = MessageHeader.of(
                MessageHeader.Type.RES_PULL, 
                header.getRequestId(),
                record != null ? record.getLength() : -1
            );

            return new Result(resHeader, record);
        } else {
            System.out.println("[debug] TopicManager.process() - REQ_PUSH 처리...");
            Topic topic = topicTable.get(message.getTopicName());
            if (topic != null) {
                topic.push(message.retain());
                System.out.println("[debug] TopicManager.process() - 메시지 저장 완료");
            }
        }

        return null;
    }
}
