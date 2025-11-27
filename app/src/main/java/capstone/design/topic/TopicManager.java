package capstone.design.topic;

import capstone.design.message.Message;
import capstone.design.message.MessageCleaner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import capstone.design.message.MessageProcessor;
import capstone.design.message.MessageType;

import org.jspecify.annotations.Nullable;

import io.netty.channel.ChannelHandlerContext;

public class TopicManager implements MessageProcessor {

    // static field =====
    private static final long DEFAULT_CLEAN_INTERVAL = 3 * (60 * 1000); // 3분

    // field =====
    private final Map<String, Topic> topics = new ConcurrentHashMap<>();
    private final MessageCleaner cleaner;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // constructor =====
    private TopicManager(Map<String, Topic> topics, long cleanInterval) {
        this.topics.putAll(topics);

        this.cleaner = new MessageCleaner(this.topics.values(), cleanInterval);
        this.cleaner.start();
    }

    // static method =====
    public static TopicManager of(Map<String, Topic> topics) { return new TopicManager(topics, DEFAULT_CLEAN_INTERVAL); }
    public static TopicManager of(Map<String, Topic> topics, long cleanInterval) { return new TopicManager(topics, cleanInterval); }

    // getter =====
    public @Nullable Topic topic(String name) { return topics.get(name); }

    // public method =====
    public void shutdownNow() { cleaner.shutdownNow(); }

    // override =====
    @Override
    public void process(ChannelHandlerContext context, Message message) {
        Map<MessageType, BiConsumer<ChannelHandlerContext, Message>> handlers = Map.of(
            MessageType.REQ_PUSH, this::push,
            MessageType.REQ_PULL, this::pull,
            MessageType.REQ_FIND, this::find,
            MessageType.REQ_SEEK, this::seek
        );

        BiConsumer<ChannelHandlerContext, Message> handler = handlers.get(message.type());
        if (handler == null) {
            return;
        }

        handler.accept(context, message);
    }

    // private method =====
    private void push(ChannelHandlerContext context, Message message) {
        Message.Builder builder = Message.builder()
            .type(MessageType.RES_PUSH)
            .header(message.header());

        try {
            Topic topic = topic(message);
            if (topic == null) {
                throw new Exception("topic 없음");
            }

            int offset = topic.push(message);
            if (offset < 0) {
                throw new Exception("메시지 저장 실패");
            }

            builder.header("offset", String.valueOf(offset));
        } catch (Exception e) {
            System.err.println("? TopicManager.push(): " + e);
            builder.header("error", e.getMessage());
        }

        context.channel().writeAndFlush(builder.build());
    }

    private void pull(ChannelHandlerContext context, Message message) {
        message.setType(MessageType.RES_PULL);

        int count = message.header("count", 1);
        long timeout = message.header("timeout", 0L);
        Topic topic = topic(message);
        if (topic == null) {
            message.addHeader("error", "topic is null");
            for (int i = 0; i < count; i++) {
                context.channel().write(message);
            }

            context.channel().flush();
            return;
        }

        List<Message> pulled = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger subscribeKey = new AtomicInteger();
        AtomicBoolean cancel = new AtomicBoolean(false);
        Runnable write = () -> {
            pulled.forEach(msg -> {
                msg.setType(MessageType.RES_PULL)
                    .addHeader(message.header());

                context.channel().write(msg);
            });

            // 부족한 개수만큼 실패 메시지 전송
            for (int i = pulled.size(); i < count; i++) {
                message.addHeader("error", "메시지 획득 실패");
                context.channel().write(message);
            }
            context.channel().flush();
        };
        Supplier<Boolean> callback = new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                // 현재 토픽/파티션에 남아있는 메시지 개수와 요청한 개수를 비교하여 메시지 획득
                pulled.addAll(pull(topic, message, Math.min(count, topic.count(message))));
                if (cancel.get()) {
                    return false;
                }

                if (pulled.size() < count) {
                    int key = topic.subscribe(message, this);
                    subscribeKey.set(key);

                    return true;
                }

                write.run();

                return true;
            }
        };  

        // 구독 해제 예약
        scheduler.schedule(() -> {
            cancel.set(true);
            topic.unsubscribe(message, subscribeKey.get());
            write.run();
        }, timeout, TimeUnit.MILLISECONDS);

        // 최초 호출(이후 구독 진행시 내부적으로 재구독 동작)
        callback.get();
    }

    private void seek(ChannelHandlerContext context, Message message) {
        Message.Builder builder = Message.builder()
            .type(MessageType.RES_SEEK)
            .header(message.header());
        try {
            Topic topic = topic(message);
            if (topic == null) {
                throw new NullPointerException("topic is null");
            }

            boolean ok = topic.seek(message);
            if (!ok) {
                throw new IllegalStateException("seek 실패");
            }
        } catch (Exception e) {
            System.err.println("? TopicManager.seek(): " + e);
            builder.header("error", e.getMessage());
        }

        context.channel().writeAndFlush(builder.build());
    }

    private void find(ChannelHandlerContext context, Message message) {
        Topic topic = topic(message);
        if (topic == null) {
            System.err.println("? TopicManager.find(): 토픽 없음");
            return;
        }

        long timeout = Long.parseLong(message.header("timeout", "0"));
        AtomicInteger subscribeKey = new AtomicInteger();
        AtomicBoolean cancel = new AtomicBoolean(false);
        Consumer<Integer> write = (offset) -> {
            message.setType(MessageType.RES_FIND)
                .addHeader("offset", String.valueOf(offset));

            if (offset < 0) {
                message.addHeader("error", "find 실패");
            }

            context.channel().writeAndFlush(message);
        };
        Supplier<Boolean> callback = new Supplier<Boolean>() {
            @Override
            public Boolean get() {
                int offset = topic.find(message);
                if (cancel.get()) {
                    return false;
                }

                if (offset < 0) {
                    int key = topic.subscribe(message, this);
                    subscribeKey.set(key);
                    return true;
                }

                write.accept(offset);

                return true;
            }
        };

        scheduler.schedule(() -> {
            cancel.set(true);
            topic.unsubscribe(message, subscribeKey.get());
            write.accept(-1);
        }, timeout, TimeUnit.MILLISECONDS);

        callback.get();
    }

    /**
     * 토픽/파티션에서 count 만큼 pull 시도.
     * 다만, 메시지가 만료되는 등 실패 가능성이 있기 때문에 반환 메시지의 개수는 count보다 작을 수 있음.
     */
    private List<Message> pull(Topic topic, Message message, int count) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TopicRecord record = topic.pull(message);
            if (record != null) {
                messages.add(record.message());
            }
        }

        return messages;
    }

    private @Nullable Topic topic(Message message) {
        String topicName = message.header("topic.name", "");
        if (topicName.isEmpty()) {
            System.err.println("? TopicManager.topic(): topic name 없음");
            return null;
        }

        return topics.get(topicName);
    }
}
