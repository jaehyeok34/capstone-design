package capstone.design.client;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import capstone.design.Utils;
import capstone.design.message.Message;
import capstone.design.message.MessageOption;
import capstone.design.message.MessageType;
import capstone.design.netty.client.NettyClient;

public class Producer implements AutoCloseable {

    private final NettyClient client;

    public Producer(String host, int port, String clientId) throws Exception {
        this.client = new NettyClient(host, port, clientId);
    }
    public Producer(String host, int port) throws Exception { this(host, port, null); }

    /**
     * 서버에 REQ_PUSH 메시지 보내고 바로 반환
     */
    public void asyncProducer(Message message) {
        Utils.validate(message);
        client.command(message);
    }

    public void asyncProduce(String topicName, int partition, byte[] payload) {
        Utils.validate(topicName, payload);
        asyncProducer(createMessage(topicName, partition, payload));
    }

    public void asyncProduce(String topicName, int partition, String payload) {
        Utils.validate(payload);
        asyncProduce(topicName, partition, payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 서버에 REQ_PUSH 메시지 보내고, 서버에서 응답이 올 때까지 대기
     */
    public Message syncProduce(Message message) {
        Utils.validate(message);
        return client.request(message).join();
    }

    public Message syncProduce(String topicName, int partition, byte[] payload) {
        Utils.validate(topicName, payload);
        return syncProduce(createMessage(topicName, partition, payload));
    }

    public Message syncProduce(String topicName, int partition, String payload) {
        Utils.validate(payload);
        return syncProduce(topicName, partition, payload.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }

    private Message createMessage(String topicName, int partition, byte[] payload) {
        return Message.of(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.REQ_PUSH.getByte(),
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition,
            MessageOption.PAYLOAD, payload
        ));
    }
}