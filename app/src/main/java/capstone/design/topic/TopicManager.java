package capstone.design.topic;

import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageCleaner;
import capstone.design.message.MessageOption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import capstone.design.message.MessageProcessor;
import capstone.design.message.MessageType;
import org.jspecify.annotations.Nullable;

import io.netty.channel.ChannelHandlerContext;

public class TopicManager implements MessageProcessor {

    private static final long DEFAULT_CLEAN_INTERVAL = 5 * (60 * 1000); // 5분

    private final Map<String, Topic> topicMap = new ConcurrentHashMap<>();
    private final MessageCleaner cleaner;

    private TopicManager(Map<String, Topic> topics, long cleanInterval) {
        try {
            topicMap.putAll(topics);
        } catch (Exception e) { System.err.println("TopicManager.<init>(): " + e); }

        // 메시지 클리너 시작
        cleaner = new MessageCleaner(topicMap.values(), cleanInterval);
        cleaner.start();
    }

    public static TopicManager of(Map<String, Topic> topics) {
        return new TopicManager(topics, DEFAULT_CLEAN_INTERVAL);
    }

    public static TopicManager of(Map<String, Topic> topics, long cleanInterval) {
        if (cleanInterval < 0) {
            cleanInterval = DEFAULT_CLEAN_INTERVAL;
        }

        return new TopicManager(topics, cleanInterval);
    }

    @Nullable
    public Topic topic(String name) { return topicMap.get(name); }

    public void close() {
        cleaner.shutdownNow();
    }

    @Override
    public void process(ChannelHandlerContext context, Message message) {
        Utils.validate(context, message);

        Byte type = message.optionAsByte(MessageOption.MESSAGE_TYPE);
        if (type == null) {
            return;
        }

        switch (MessageType.values()[type]) {
            case REQ_PUSH -> push(context, message);
            case REQ_PULL -> pull(context, message);
            case REQ_SUBSCRIBE -> subscribe(context, message);
            case REQ_UNSUBSCRIBE -> unsubscribe(context, message);
            default -> {}
        }
    }

    private void push(ChannelHandlerContext context, Message message) {
        String topicName = message.optionAsString(MessageOption.TOPIC_NAME);
        Integer partition = message.optionAsInt(MessageOption.PARTITION);
        String clientId = message.optionAsString(MessageOption.CLIENT_ID);
        byte[] payload = message.optionAsBytes(MessageOption.PAYLOAD);

        message.addOptions(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.RES_PUSH.getByte(),
            MessageOption.OK, 0 // 일단 실패로 초기화
        )).removeOptions(MessageOption.PAYLOAD); // payload 받았으니까 다음 전송을 위해 제거
        
        // 메시지에서 꺼내온 값들이 유효하면 비즈니스 로직 수행
        if(Utils.isValid(topicName, partition, clientId, payload)) {
            Topic topic = topicMap.get(topicName);
            if (topic != null && topic.push(partition, clientId, payload)) {
                message.addOptions(Map.of(
                    MessageOption.OFFSET, topic.offset(partition, clientId),
                    MessageOption.COUNT, topic.count(partition, clientId),
                    MessageOption.OK, 1
                ));

                topic.notify(partition, message.copy());
            }
        }

        context.channel().writeAndFlush(message);
    }

    private void pull(ChannelHandlerContext context, Message message) {
        String topicName = message.optionAsString(MessageOption.TOPIC_NAME);
        Integer partition = message.optionAsInt(MessageOption.PARTITION);
        String clientId = message.optionAsString(MessageOption.CLIENT_ID);
        Long offset = message.optionAsLong(MessageOption.OFFSET);

        message.addOptions(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.RES_PULL.getByte(),
            MessageOption.OK, 0 // 일단 실패로 초기화
        )).removeOptions(MessageOption.PAYLOAD); // payload 제거(당연히 없겠지만 안전하게)

        // 메시지에서 꺼내온 값들이 유효하면 비즈니스 로직 수행
        if (Utils.isValid(topicName, partition, clientId)) {
            Topic topic = topicMap.get(topicName);
            TopicRecord record;
            if (topic != null && (record = topic.pull(partition, clientId, offset)) != null) {
                message.addOptions(Map.of(
                    MessageOption.PAYLOAD, record,
                    MessageOption.OFFSET, topic.offset(partition, clientId),
                    MessageOption.COUNT, topic.count(partition, clientId),
                    MessageOption.OK, 1
                ));
            }
        }

        context.channel().writeAndFlush(message);
    }

    private void subscribe(ChannelHandlerContext context, Message message) {
        String topicName = message.optionAsString(MessageOption.TOPIC_NAME);
        Integer partition = message.optionAsInt(MessageOption.PARTITION);
        String clientId = message.optionAsString(MessageOption.CLIENT_ID);

        message.addOptions(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.RES_SUBSCRIBE.getByte(),
            MessageOption.OK, 0 // 일단 실패로 초기화
        ));

        if (Utils.isValid(topicName, partition, clientId)) {
            Topic topic = topicMap.get(topicName);
            if (topic != null) {
                topic.subscribe(context, partition, clientId);
                message.addOptions(Map.of(
                    MessageOption.OFFSET, topic.offset(partition, clientId),
                    MessageOption.COUNT, topic.count(partition, clientId),
                    MessageOption.OK, 1
                ));
            }
        }

        context.channel().writeAndFlush(message);
    }

    private void unsubscribe(ChannelHandlerContext context, Message message) {
        String topicName = message.optionAsString(MessageOption.TOPIC_NAME);
        Integer partition = message.optionAsInt(MessageOption.PARTITION);
        String clientId = message.optionAsString(MessageOption.CLIENT_ID);

        message.addOptions(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.RES_UNSUBSCRIBE.getByte(),
            MessageOption.OK, 0 // 일단 실패로 초기화
        ));

        if (Utils.isValid(topicName, partition, clientId)) {
            Topic topic = topicMap.get(topicName);
            if (topic != null) {
                topic.unsubscribe(partition, clientId);
                message.addOption(MessageOption.OK, 1);
            }
        }

        context.channel().writeAndFlush(message);
    }
}
