package capstone.design.message;

public enum MessageType {
    REQ_PULL, RES_PULL, // topic/partition에서 메시지 요청
    REQ_PUSH, RES_PUSH, // topic/partition에 메시지 저장
    REQ_SUBSCRIBE, RES_SUBSCRIBE, // topic/partition 구독
    REQ_UNSUBSCRIBE, RES_UNSUBSCRIBE, // topic/partition 구독 취소
    TOPIC_UPDATED; // 구독자에게 topic이 업데이트 됐음을 알림

    public byte getByte() { return (byte) this.ordinal(); }
    public static int SIZE = Byte.BYTES;
}
