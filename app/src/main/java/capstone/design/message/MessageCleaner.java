package capstone.design.message;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import capstone.design.topic.Topic;

public class MessageCleaner {

    private final ExecutorService service;
    private final Collection<Topic> topics;
    private final long interval;

    public MessageCleaner(Collection<Topic> topics, long interval) {
        service = Executors.newSingleThreadExecutor();
        this.topics = topics;
        this.interval = interval;
    }

    public void start() {
         service.submit(() -> {
            while (true) {
                try {
                    Thread.sleep(interval);
                    for (Topic topic : topics) {
                        topic.clean();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("? MessageCleaner: " + e);
                }
            }
        });
    }

    public void shutdownNow() {
        service.shutdownNow();
    }
}
