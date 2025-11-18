package capstone.design.client;

public class Agent implements AutoCloseable {

    private final Producer producer;
    private final Consumer consumer;
    
    // constructor =====
    private Agent(Producer producer, Consumer consumer) {
        this.producer = producer;
        this.consumer = consumer;
    }

    // factory method =====
    public static Agent of(String host, int port, String clientId) throws Exception {
        return new Agent(
            new Producer(host, port, clientId),
            new Consumer(host, port, clientId)
        );
    }

    public static Agent of(Producer producer, Consumer consumer) {
        return new Agent(producer, consumer); 
    }

    // getter =====
    public Producer producer() { return producer; }
    public Consumer consumer() { return consumer; }

    @Override
    public void close() throws Exception {
        producer.close();
        consumer.close();
    }
}
