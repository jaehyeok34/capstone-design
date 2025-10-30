package capstone.design.topic.subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import io.netty.channel.ChannelHandlerContext;

public class SubscriberManager {

    private final Map<Integer, List<Subscriber>> subscriberTable = new ConcurrentHashMap<>();
    private final String name;

    public SubscriberManager(String name) {
        Utils.validate(name);
        
        this.name = name;
    }

    public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
        Utils.validate(context, clientId);

        subscriberTable.computeIfAbsent(partition, ignored -> new ArrayList<>())
            .add(new Subscriber(context, clientId));
    }

    public void unsubscribe(int partition, String clientId) {
        Utils.validate(clientId);

        List<Subscriber> subscribers = subscriberTable.get(partition);
        if (subscribers == null) {
            return;
        }

        subscribers.removeIf(subscriber -> subscriber.clientId().equals(clientId));
    }

    public boolean notify(int partition) { return notify(partition, -1, -1, -1); }
    public boolean notify(int partition, long cursor) { return notify(partition, cursor, -1, -1); }
    public boolean notify(int partition, long cursor, long offset) { return notify(partition, cursor, offset, -1); }
    public boolean notify(int partition, long cursor, long offset, long remaining_count) {
        Message message = new Message().addOption(MessageOption.PARTITION, partition);

        if (cursor >= 0) message.addOption(MessageOption.CURSOR, cursor);
        if (offset >= 0) message.addOption(MessageOption.OFFSET, offset);
        if (remaining_count >= 0) message.addOption(MessageOption.REMAINING_COUNT, remaining_count);
        
        return notify(partition, message);
    }

    public boolean notify(int partition, Message message) {
        List<Subscriber> subscribers = subscriberTable.get(partition);
        if (subscribers == null) {
            return false;
        }

        for (Subscriber subscriber : subscribers) {
            ChannelHandlerContext context = subscriber.context();
        
            message.addOptions(Map.of(
                MessageOption.MESSAGE_TYPE, MessageType.TOPIC_UPDATED.getByte(),
                MessageOption.TOPIC_NAME, name,
                MessageOption.CLIENT_ID, subscriber.clientId()
            )).removeOptions(
                MessageOption.PAYLOAD,
                MessageOption.REQUEST_ID
            ); // payload 제거(당연히 없겠지만 안전하게)
            
            context.channel().writeAndFlush(message);
        }

        return true;
    }

    public int count(int partition) {
        List<Subscriber> subscribers = subscriberTable.get(partition);
        if (subscribers == null) {
            return 0;
        }

        return subscribers.size();
    }
}
