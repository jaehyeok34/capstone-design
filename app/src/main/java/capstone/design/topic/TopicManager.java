package capstone.design.topic;

import capstone.design.message.Message;
import capstone.design.message.MessageCleaner;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
        switch (message.type()) {
            case REQ_PUSH -> push(context, message);
            case REQ_PULL -> pull(context, message);
            case REQ_FIND -> find(context, message);
            case REQ_SEEK -> seek(context, message);
            default -> {}
        }
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

            builder.header("ok", "true")
                .header("offset", String.valueOf(offset));
        } catch (Exception e) {
            System.err.println("! TopicManager.push(): " + e);
            builder.header("ok", "false");
        }

        context.channel().writeAndFlush(builder.build());
    }

    private void pull(ChannelHandlerContext context, Message message) {
        int count = Integer.parseInt(message.header("count", "1"));
        long timeout = Long.parseLong(message.header("timeout", "0"));
        List<Message> results = new ArrayList<>();

        try {
            Topic topic = topic(message);
            if (topic == null) {
                throw new Exception("토픽 없음");
            }

            int existingCount = topic.count(message);
            List<Message> pulled = pull(topic, message, Math.min(count, existingCount));
            if (pulled.size() < count && timeout > 0) {
                subscribe( // 구독하여 부족한 메시지 채우기
                    topic, message, timeout, 
                    () -> {
                        TopicRecord record = topic.pull(message);
                        if (record != null) {
                            pulled.add(record.message());
                        }
                    }, 
                    () -> pulled.size() < count
                );
            }

            for (Message msg : pulled) {
                msg.setType(MessageType.RES_PULL)
                    .addHeader("ok", "true")
                    .addHeader(message.header());
                results.add(msg);
            }
        } catch (Exception e) {
            System.err.println("! TopicManager.pull(): " + e);
        }

        // 토픽에서 획득한 메시지 write
        for (Message msg : results) {
            context.channel().write(msg);
        }

        // 부족한 개수만큼 실패 메시지 write
        message.setType(MessageType.RES_PULL).addHeader("ok", "false");
        for (int i = results.size(); i < count; i++) {
            context.channel().write(message);
        }

        context.channel().flush();
    }

    private void seek(ChannelHandlerContext context, Message message) {
        Topic topic = topic(message);
        if (topic == null) {
            System.err.println("! TopicManager.seek(): 토픽 없음");
            return;
        }

        boolean ok = topic.seek(message);
        message.setType(MessageType.RES_SEEK).addHeader("ok", Boolean.toString(ok));
        context.channel().writeAndFlush(message);
    }

    private void find(ChannelHandlerContext context, Message message) {
        Topic topic = topic(message);
        if (topic == null) {
            System.err.println("! TopicManager.find(): 토픽 없음");
            return;
        }

        long timeout = Long.parseLong(message.header("timeout", "0"));
        AtomicInteger offset = new AtomicInteger(topic.find(message));

        /*
         * 다음 조건을 모두 만족하면, 구독 및 반복 조회 수행
         * 1. 찾은 offset이 음수(유효하지 않은 값)
         * 2. timeout이 설정 됨(0 초과)
         */
        if (offset.get() < 0 && timeout > 0) {
            subscribe(
                topic, message, timeout, 
                () -> offset.set(topic.find(message)), // callback
                () -> offset.get() < 0 // condition
            );
        }

        message.setType(MessageType.RES_FIND).addHeader("offset", String.valueOf(offset.get()));
        context.channel().writeAndFlush(message);
    }

    /**
     * 특정 토픽/파티션을 구독하여, 다음 조건이 만족될 때 까지 대기하며 callback 실행
     * 1. strict 모드가 아닐 경우 한 번만 대기 후 callback 실행
     * 2. strict 모드이면서 남은 timeout이 존재하다면 condition이 true를 반환할 때까지 반복 대기
     * 
     * 반환 직전 unsubscribe 수행함
     * 
     * @param callback 구독한 토픽/파티션에 메시지가 갱신됐을 경우 실행할 콜백 함수
     */
    private void subscribe(Topic topic, Message message, long timeout, Runnable callback, Supplier<Boolean> condition) {
        BlockingQueue<Object> notifiedQueue = new LinkedBlockingQueue<>();
        int key = topic.subscribe(message, notifiedQueue);
        System.out.println("! TopicManager.subscribe(): 구독 시작, key=" + key);

        do {
            long start = System.currentTimeMillis();

            try {
                notifiedQueue.poll(timeout, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}

            callback.run();
            timeout -= (System.currentTimeMillis() - start);
        } while(timeout > 0 && condition.get());

        topic.unsubscribe(message, key);
        System.out.println("! TopicManager.subscribe(): 구독 해제 완료, key=" + key);
    }

    /**
     * 이를 호출하기 전에, {@code topic.count()}를 통해 동일한 조건(group, offset)의 메시지 개수를 획득하여
     * {@code count}로 전달하기 때문에, 실제 {@code topic.pull()}에서 {@code null}이 반환되는 경우는 없을 것으로 예상
     * 다만, 완전하지 않으므로 반환하는 메시지 리스트의 개수는 {@code count}보다 작을 수 있으므로, 개수를 보장하지 않음
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
            System.err.println("! TopicManager.topic(): topic name 없음");
            return null;
        }

        return topics.get(topicName);
    }
}
