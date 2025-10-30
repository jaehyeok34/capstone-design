package capstone.design.topic;

import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import capstone.design.message.MessageProcessor;
import capstone.design.message.MessageType;
import capstone.design.topic.disk.DiskTopic;
import capstone.design.topic.memory.MemoryTopic;
import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class TopicManager implements MessageProcessor {

    private final Map<String, Topic> topicTable = new ConcurrentHashMap<>();

    public TopicManager(Map<String, Topic.Type> topicInfo) {
        topicInfo.forEach((name, type) -> {
            try {
                Utils.validate(name, type);

                Topic topic = (type == Topic.Type.MEMORY) ? 
                    new MemoryTopic(name) : new DiskTopic(name);
                topicTable.put(name, topic);
            } catch (Exception ignored) { return; }
        });
    }

    @Nullable
    public Topic topic(String name) { return topicTable.get(name); }

    @Override
    public void process(ChannelHandlerContext context, Message message) {
        Utils.validate(context, message);

        Byte type = message.option(MessageOption.MESSAGE_TYPE, Byte.class);
        Utils.validate(type);

        switch (MessageType.values()[type]) {
            case REQ_PUSH -> push(context, message);
            case REQ_PULL -> pull(context, message);
            case REQ_SUBSCRIBE -> subscribe(context, message);
            case REQ_UNSUBSCRIBE -> unsubscribe(context, message);
            default -> throw new IllegalStateException("알 수 없는 타입");
        }
    }

    private void push(ChannelHandlerContext context, Message message) {
        String clientId = message.option(MessageOption.CLIENT_ID, String.class);
        String topicName = message.option(MessageOption.TOPIC_NAME, String.class);
        Integer partition = message.option(MessageOption.PARTITION, Integer.class);
        ByteBuf payload = message.option(MessageOption.PAYLOAD, ByteBuf.class);

        Utils.validate(clientId, topicName, partition, payload);
        
        message.addOption(MessageOption.MESSAGE_TYPE, MessageType.RES_PUSH.getByte()) // type 변경
            .removeOptions(MessageOption.PAYLOAD); // payload 받았으니까 다음 전송을 위해 제거

        Topic topic = topicTable.get(topicName);
        if (topic != null) {
            boolean ok = topic.push(partition, clientId, payload.retain());
            if (ok) {
                message.addOptions(Map.of(
                    MessageOption.CURSOR, topic.cursor(partition, clientId),
                    MessageOption.OFFSET, topic.offset(partition, clientId),
                    MessageOption.REMAINING_COUNT, topic.remainingCount(partition, clientId)
                ));
                    
                topic.notify(partition, message.copy()); // 구독자들에게 알림
            }
        }

        context.channel().writeAndFlush(message);
    }

    private void pull(ChannelHandlerContext context, Message message) {
        String clientId = message.option(MessageOption.CLIENT_ID, String.class);
        String topicName = message.option(MessageOption.TOPIC_NAME, String.class);
        Integer partition = message.option(MessageOption.PARTITION, Integer.class);
        Long cursor = message.option(MessageOption.CURSOR, Long.class);
        cursor = (cursor != null) ? cursor : -1L;
        
        Utils.validate(clientId, topicName, partition);

        message.addOption(MessageOption.MESSAGE_TYPE, MessageType.RES_PULL.getByte()) // type 변경
            .removeOptions(MessageOption.PAYLOAD); // payload 제거(당연히 없겠지만 안전하게)

        Topic topic = topicTable.get(topicName);
        if (topic != null) {
            TopicRecord record = topic.pull(partition, clientId, cursor);
            if (record != null) {
                message.addOptions(Map.of(
                    MessageOption.PAYLOAD, record,
                    MessageOption.CURSOR, topic.cursor(partition, clientId),
                    MessageOption.OFFSET, topic.offset(partition, clientId),
                    MessageOption.REMAINING_COUNT, topic.remainingCount(partition, clientId)
                ));
            }
        }

        context.channel().writeAndFlush(message);
    }

    private void subscribe(ChannelHandlerContext context, Message message) {
        String clientId = message.option(MessageOption.CLIENT_ID, String.class);
        String topicName = message.option(MessageOption.TOPIC_NAME, String.class);
        Integer partition = message.option(MessageOption.PARTITION, Integer.class);

        Utils.validate(clientId, topicName, partition);

        Topic topic = topicTable.get(topicName);
        if (topic != null) {
            topic.subscribe(context, partition, clientId);
            message.addOptions(Map.of(
                MessageOption.CURSOR, topic.cursor(partition, clientId),
                MessageOption.OFFSET, topic.offset(partition, clientId),
                MessageOption.REMAINING_COUNT, topic.remainingCount(partition, clientId)
            ));
        }

        message.addOption(MessageOption.MESSAGE_TYPE, MessageType.RES_SUBSCRIBE.getByte());
        context.channel().writeAndFlush(message);
    }

    private void unsubscribe(ChannelHandlerContext context, Message message) {
        String id = message.option(MessageOption.CLIENT_ID, String.class);
        String topicName = message.option(MessageOption.TOPIC_NAME, String.class);
        Integer partition = message.option(MessageOption.PARTITION, Integer.class);

        Utils.validate(id, topicName, partition);

        Topic topic = topicTable.get(topicName);
        if (topic != null) {
            topic.unsubscribe(partition, id);
        }

        message.addOption(MessageOption.MESSAGE_TYPE, MessageType.RES_UNSUBSCRIBE.getByte());
        context.channel().writeAndFlush(message);
    }
}
