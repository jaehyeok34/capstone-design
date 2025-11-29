package capstone.design.message;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import capstone.design.topic.Topic;

public class MessageCleaner {

    private final Collection<Topic> topics;
    private final long interval;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> future;

    public MessageCleaner(Collection<Topic> topics, long interval) {
        this.topics = topics;
        this.interval = interval;
    }

    public void start() {
        future = scheduler.scheduleAtFixedRate(() -> {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            topics.forEach(topic -> {
                topic.clean();
            });
        }, interval, interval, TimeUnit.MILLISECONDS);
    }

    public void shutdownNow() {
        if (future != null) {
            future.cancel(true);
        }

        scheduler.shutdownNow();
    }
}
