package capstone.design.topic.subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;

import io.netty.channel.ChannelHandlerContext;

public class SubscriberManager {

    private final Map<Integer, List<Subscriber>> subscriberTable = new HashMap<>();
    private final String name;

    public SubscriberManager(String name) {
        Utils.validate(name);
        
        this.name = name;
    }

    public void subscribe(ChannelHandlerContext context, int partition, String id) {
        Utils.validate(context, id);

        subscriberTable.computeIfAbsent(partition, k -> new ArrayList<>())
            .add(new Subscriber(context, id));
    }

    public void unsubscribe(int partition, String id) {
        Utils.validate(id);

        List<Subscriber> subscribers = subscriberTable.get(partition);
        if (subscribers == null) {
            return;
        }

        subscribers.removeIf(subscriber -> subscriber.id().equals(id));
    }

    public void notify(int partition) { notify(partition, -1); } 
    public void notify(int partition, long cursor) { notifyTo(partition, null, cursor); }
    public void notifyTo(int partition, String id) { notifyTo(partition, id, -1); }
    public void notifyTo(int partition, String id, long cursor) {
        List<Subscriber> subscribers = subscriberTable.get(partition);
        if (subscribers == null) {
            return;
        }

        subscribers.stream()
            .filter(subscriber -> { // id를 지정하면 그 id만, null이면 전체
                if (id == null) {
                    return true;
                }

                return subscriber.id().equals(id);
            }).forEach(subscriber -> {
                ChannelHandlerContext context = subscriber.context();

                Message message = new Message()
                    .addOption(MessageOption.TYPE, Message.Type.TOPIC_UPDATE.getByte())
                    .addOption(MessageOption.ID, subscriber.id())
                    .addOption(MessageOption.TOPIC_NAME, name)
                    .addOption(MessageOption.PARTITION, partition)
                    .addOption(MessageOption.CURSOR, cursor);

                context.channel().writeAndFlush(message); // message encoder로 전달
            });
    }

    public int count(int partition) {
        return Optional.ofNullable(subscriberTable.get(partition))
            .map(List::size)
            .orElse(0);
    }
}
