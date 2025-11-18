package capstone.design.message;

public enum MessageType {
    REQ_PULL, RES_PULL, // topic/partition에서 메시지 요청
    REQ_PUSH, RES_PUSH, // topic/partition에 메시지 저장
    REQ_FIND, RES_FIND, // topic/partition에서 메시지 검색
    REQ_SEEK, RES_SEEK; // topic/partition에서 특정 오프셋으로 이동

    public byte getByte() { return (byte) this.ordinal(); }
    public static int SIZE = Byte.BYTES;
}
