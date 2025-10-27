package org.example.client;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.example.Utils;
import org.example.message.Message;
import org.example.message.MessageOption;
import org.example.netty.client.NettyClient;

public class Producer implements AutoCloseable {

    private final NettyClient client;
    private final String id;

    public Producer(String host, int port, String id) throws Exception {
        this.client = new NettyClient(host, port);
        this.id = (id != null && !id.isEmpty()) ? id : NettyClient.DEFAULT_ID;
    }
    public Producer(String host, int port) throws Exception { this(host, port, null); }

    private Message createMessage(String topicName, int partition, byte[] payload) {
        Utils.validate(topicName, payload);

        return new Message().addOptions(Map.of(
            MessageOption.TYPE, Message.Type.REQ_PUSH.getByte(),
            MessageOption.ID, id,
            MessageOption.TOPIC_NAME, topicName,
            MessageOption.PARTITION, partition,
            MessageOption.PAYLOAD, payload
        ));
    }

    /**
     * 서버에 REQ_PUSH 메시지 보내고 바로 반환
     */
    public void asyncProduce(String topicName, int partition, byte[] payload) {
        Message message = createMessage(topicName, partition, payload);
        client.command(message);
    }

    public void asyncProduce(String topicName, int partition, String payload) {
        Utils.validate(payload);
        asyncProduce(topicName, partition, payload.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 서버에 REQ_PUSH 메시지 보내고, 서버에서 응답이 올 때까지 대기
     */
    public void syncProduce(String topicName, int partition, byte[] payload) {
        Message message = createMessage(topicName, partition, payload);

        /*
         * 서버의 응답 대기
         * 현재는 응답 메시지를 해석조차 하지 않음. 즉,
         * 서버에서 RES_PUSH 메시지 이외의 메시지가 오더라도 일단은 RES_PUSH 메시지가 왔다고 간주함
         */
        client.request(message).join(); 
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