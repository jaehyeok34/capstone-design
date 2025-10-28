package capstone.design.message;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import capstone.design.Utils;
import capstone.design.topic.TopicRecord;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

// 추가하면 좋을 것:
// magic(내가 정의한 프로토콜이 맞는지 확인하기 위한 값, ex: 0xCFFA...)
// version(프로토콜 버전)
// checksum(무결성 확인)
public class Message {

    private final Map<String, Object> options = new HashMap<>();
    
    public Message addOption(String key, Object value) {
        Utils.validate(key, value);
        options.put(key, value);

        return this;
    }

    public Message addOptions(Map<String, Object> options) {
        Utils.validate(options);
        options.forEach(this::addOption);

        return this;
    }

    public Message removeOption(String key) {
        Utils.validate(key);
        options.remove(key);

        return this;
    }

    public <T> T option(String key, Class<T> type) {
        Utils.validate(key, type);
        Object value = options.get(key);
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        
        return null;
    }

    public Object option(String key) {
        return options.get(key);
    }

    public Map<String, Object> options() {
        return options;
    }

    public void clearOptions() {
        options.clear();
    }

    /**
     * type, id, topic name, partition, offset, payload 정보를 ByteBuf로 변환
     * [type][length + id][length + topic name][partition][offset][length + (payload)]
     * 여기서 payload가 DiskRecord인 경우 length까지만 기록되고 실제 payload 데이터는 기록되지 않음
     */
    public ByteBuf toByteBuf() {
        ByteBuf buf = Unpooled.buffer();

        Byte type = option(MessageOption.TYPE, Byte.class);
        Utils.validate(type);
        buf.writeByte(type);

        String id = option(MessageOption.ID, String.class);
        writeString(buf, id);

        String topicName = option(MessageOption.TOPIC_NAME, String.class);
        writeString(buf, topicName);

        Integer partition = option(MessageOption.PARTITION, Integer.class);
        buf.writeInt((partition != null) ? partition : Integer.MAX_VALUE);

        Long cursor = option(MessageOption.CURSOR, Long.class);
        buf.writeLong((cursor != null) ? cursor : -1);

        Object payload = option(MessageOption.PAYLOAD, Object.class);
        if (payload == null) {
            buf.writeInt(-1); // payload가 없을 경우 길이에 -1 기록
            return buf;
        }

        switch (payload) {
            case byte[] b -> buf.writeInt(b.length).writeBytes(b);
            case ByteBuf b -> buf.writeInt(b.readableBytes()).writeBytes(b);
            case TopicRecord r -> buf.writeInt(r.length());
            default -> buf.writeInt(-1); // 그 외(payload가 없을 경우) 길이에 -1 기록
        }

        return buf;
    }

    private void writeString(ByteBuf buf, String string) {
        buf.writeInt(string.length()).writeBytes(string.getBytes(StandardCharsets.UTF_8));
    }

    public static enum Type {
        REQ_PULL, RES_PULL, // topic/partition에서 메시지 요청
        REQ_PUSH, RES_PUSH, // topic/partition에 메시지 저장
        REQ_SUBSCRIBE, RES_SUBSCRIBE, // topic/partition 구독
        REQ_UNSUBSCRIBE, RES_UNSUBSCRIBE, // topic/partition 구독 취소
        TOPIC_UPDATE; // 구독자에게 topic이 업데이트 됐음을 알림

        public byte getByte() { return (byte) this.ordinal(); }
        public static int SIZE = Byte.BYTES;
    }
}
