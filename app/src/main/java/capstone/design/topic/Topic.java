package capstone.design.topic;

import org.jspecify.annotations.Nullable;

import capstone.design.message.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface Topic {

    boolean push(int partition, String clientId, ByteBuf buf);
    @Nullable TopicRecord pull(int partition, String clientId, long offset);
    boolean notify(int partition, Message message);
    void subscribe(ChannelHandlerContext context, int partition, String clientId);
    void unsubscribe(int partition, String clientId);
    long length(int partition, String clientId);
    long cursor(int partition, String clientId);
    long offset(int partition, String clientId);
    long remainingCount(int partition, String clientId);

    public enum Type { MEMORY, DISK }
}
