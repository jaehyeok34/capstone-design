package capstone.design.message;

import org.jspecify.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.channel.FileRegion;

public class Frame {
    private final ByteBuf header;
    private final @Nullable Object payload; // ByteBuf or FileRegion

    private Frame(ByteBuf header, Object payload) {
        this.header = header;
        this.payload = payload;
    }

    public static Frame of(ByteBuf header, ByteBuf payload) { return new Frame(header, payload); }
    public static Frame of(ByteBuf header, FileRegion payload) { return new Frame(header, payload); }
    public static Builder builder() {  return new Frame.Builder(); }

    public ByteBuf header() { return header; }
    /**
     * @return {@code ByteBuf} or {@code FileRegion} or {@code null}
     */
    public @Nullable Object payload() { return payload; }
    public boolean isPayload() { return payload != null; }
    public int headerLength() { return header.readableBytes(); }
    public int payloadLength() { 
        switch (payload) {
            case ByteBuf buf -> {
                return buf.readableBytes();
            }

            case FileRegion region -> {
                return (int) region.count();
            }

            case null,
            default -> {
                return 0;
            }
        }
    }
    public long length() { return headerLength() + payloadLength(); }

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
