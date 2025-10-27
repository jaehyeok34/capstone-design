package capstone.design.message;

import capstone.design.topic.TopicRecord;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class MessageEncoder extends ChannelOutboundHandlerAdapter {

    public static final int MAGIC = 0x6B3FA0FF;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof Message message) {
            ByteBuf encoded = ctx.alloc().buffer();
            ByteBuf messageBuf = message.toByteBuf();
            
            encoded.writeInt(MAGIC); // magic

            /*
             * payload가 TopicRecord인 경우,
             * message.toByteBuf()에는 payload length까지 포함되고
             * 실제 payload의 길이가 추가되지 않음
             * ex) cursor 까지의 길이가 30이고, 실제 payload 길이가 6이면, 
             * toByteBuf().readableBytes() = 34임. (cursor(30) + payload length(4))
             * 
             * 따라서, 실제 payload 길이(6)을 더해줘야 함
             */
            if (message.option(MessageOption.PAYLOAD) instanceof TopicRecord record) {
                encoded.writeLong(messageBuf.readableBytes() + record.length())
                    .writeBytes(messageBuf);

                ctx.write(encoded);
                ctx.write(record.value());
            } else { // null or ByteBuf, ...
                encoded.writeLong(messageBuf.readableBytes())
                .writeBytes(messageBuf);
                
                ctx.write(encoded);
            }

            ctx.flush();
        }
    }
}