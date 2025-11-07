package capstone.design.topic.subscribe;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import io.netty.channel.ChannelHandlerContext;

public class SubscriberManager {

    // 외부 키: partition, 내부 키: clientId
    private final Map<Integer, Map<String, Subscriber>> subscriberMap = new ConcurrentHashMap<>();

    public void subscribe(ChannelHandlerContext context, int partition, String clientId) {
        Utils.validate(context, clientId);

        subscriberMap.computeIfAbsent(partition, ignored -> new ConcurrentHashMap<>())
            .put(clientId, new Subscriber(context, clientId));
    }

    public void unsubscribe(int partition, String clientId) {
        Utils.validate(clientId);

        Map<String, Subscriber> subscribers = subscriberMap.get(partition);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(clientId);
    }

    public boolean notify(int partition, Message message) {
        Map<String, Subscriber> subscribers = subscriberMap.get(partition);
        if (subscribers == null) {
            return false;
        }

        for (Subscriber subscriber : subscribers.values()) {
            message.addOption(MessageOption.MESSAGE_TYPE, MessageType.TOPIC_UPDATED.getByte())
                .removeOptions(
                    MessageOption.PAYLOAD, // 당연히 없겠지만 안전하게
                    MessageOption.REQUEST_ID // 사용하지 않음
                );

            subscriber.context().channel().writeAndFlush(message);
        }
        return true;
    }

    public int count(int partition) {
        Map<String, Subscriber> subscribers = subscriberMap.get(partition);
        if (subscribers == null) {
            return 0;
        }

        return subscribers.size();
    }
}
