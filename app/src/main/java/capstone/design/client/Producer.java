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
    private final String client_id;

    public Producer(String host, int port, String clientId, String filePath) throws Exception {
        this.client = new NettyClient(host, port, filePath);
        this.client_id = (clientId != null && !clientId.isEmpty()) ? clientId : NettyClient.DEFAULT_ID;
    }
    public Producer(String host, int port, String clientId) throws Exception { this(host, port, clientId, null); }
    public Producer(String host, int port) throws Exception { this(host, port, null, null); }

    private Message createMessage(String topicName, int partition, byte[] payload) {
        Utils.validate(topicName, payload);

        return new Message().addOptions(Map.of(
            MessageOption.MESSAGE_TYPE, MessageType.REQ_PUSH.getByte(),
            MessageOption.CLIENT_ID, client_id,
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition,
            MessageOption.PAYLOAD, payload
        ));
    }

    /**
     * 서버에 REQ_PUSH 메시지 보내고 바로 반환
     */
    public void asyncProducer(Message message) {
        Utils.validate(message);

        client.command(message);
    }

    public void asyncProduce(String topicName, int partition, byte[] payload) {
        asyncProducer(createMessage(topicName, partition, payload));
    }

    public void asyncProduce(String topicName, int partition, String payload) {
        asyncProduce(topicName, partition, payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 서버에 REQ_PUSH 메시지 보내고, 서버에서 응답이 올 때까지 대기
     */
    public void syncProduce(Message message) {
        Utils.validate(message);

        client.request(message).join();
    }
    
    public void syncProduce(String topicName, int partition, byte[] payload) {
        Message message = createMessage(topicName, partition, payload);

        /*
         * 서버의 응답 대기
         * 현재는 응답 메시지를 해석조차 하지 않음. 즉,
         * 서버에서 RES_PUSH 메시지 이외의 메시지가 오더라도 일단은 RES_PUSH 메시지가 왔다고 간주함
         */
        syncProduce(message);
    }

    public void syncProduce(String topicName, int partition, String payload) {
        Utils.validate(payload);
        syncProduce(topicName, partition, payload.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws Exception { 
        client.shutdown();
    }
}