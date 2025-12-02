package capstone.design.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.FileRegion;

public class Frame {
    private final ByteBuf header;
    private final Object payload; // ByteBuf or FileRegion

    private Frame(ByteBuf header, Object payload) {
        this.header = header;
        this.payload = payload;
    }

    public static Frame of(ByteBuf header, ByteBuf payload) { return new Frame(header, payload); }
    public static Frame of(ByteBuf header, FileRegion payload) { return new Frame(header, payload); }
    public static Builder builder() {  return new Frame.Builder(); }

    public ByteBuf header() { return header; }
    public Object payload() { return payload; }
    public boolean isPayloadFileRegion() { return payload instanceof FileRegion; }
    public int headerLength() { return header.readableBytes(); }
    public int payloadLength() { 
        if (payload == null) {
            return 0;
        }

        if (payload instanceof FileRegion region) {
            return (int) region.count();
        }

        return ((ByteBuf) payload).readableBytes();
    }
    public long length() {
        if (payload instanceof FileRegion region) {
            // header buf 길이 + FileRegion의 길이를 기록하는 buf 길기 + FileRegion의 실제 길이
            return header.readableBytes() + Integer.BYTES + region.count();
        }

        return header.readableBytes() + ((ByteBuf) payload).readableBytes();
    }

    public ByteBuf toByteBuf() {
        if (payload instanceof FileRegion region) {
            ByteBuf countBuf = Unpooled.directBuffer()
                .writeInt((int) region.count()); 

            return Unpooled.wrappedBuffer(header, countBuf);
        }

        return Unpooled.wrappedBuffer(header, (ByteBuf) payload);
    }

    static class Builder {
        private ByteBuf header = null;
        private Object payload = null;

        private Builder() {}

        public Frame build() {
            return new Frame(header, payload);
        }

        public Builder header(ByteBuf header) {
            this.header = header;
            return this;
        }

        public Builder payload(ByteBuf payload) {
            this.payload = payload;
            return this;
        }

        public Builder payload(FileRegion payload) {
            this.payload = payload;
            return this;
        }
    }
}
