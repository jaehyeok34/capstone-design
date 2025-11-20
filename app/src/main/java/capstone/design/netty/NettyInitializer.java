package capstone.design.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;

public class NettyInitializer extends ChannelInitializer<Channel> {

    private final List<Supplier<ChannelHandler>> handlerConstructors;
    private final ChannelDuplexHandler exceptionHandler;

    private NettyInitializer(Builder builder) {
        this.handlerConstructors = builder.handlerConstructors;
        this.exceptionHandler = builder.exceptionHandler;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        for (Supplier<ChannelHandler> constructor : handlerConstructors) {
            pipeline.addLast(constructor.get());
        }

        pipeline.addLast(exceptionHandler);
    }

    public static Builder builder() { return new Builder(null); }
    public static Builder builder(ChannelDuplexHandler exceptionHandler) { return new Builder(exceptionHandler); }

    public static class Builder {

        private final List<Supplier<ChannelHandler>> handlerConstructors = new ArrayList<>();
        private final ChannelDuplexHandler exceptionHandler;

        private Builder(ChannelDuplexHandler exceptionHandler) {
            this.exceptionHandler = (exceptionHandler != null) ? exceptionHandler : new ChannelDuplexHandler() {
                @Override
                public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                    ctx.close(); // 채널 비활성화 시 채널 닫기
                }

                @Override
                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                    // 예외 발생하더라도 채널을 닫지 않고 유지
                    System.err.println("채널(" + ctx.channel().remoteAddress() + ") 예외 발생: " + cause.getMessage());
                }
            };
        }

        public Builder addHandler(Class<? extends ChannelHandler> handlerClass) {
            handlerConstructors.add(() -> {
                try {
                    return handlerClass.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return this;
        }

        public Builder addHandler(Class<? extends ChannelHandler> handlerClass, Object... args) {
            Class<?>[] argTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i].getClass();
            }

            return addHandler(handlerClass, argTypes, args);
        }

        public Builder addHandler(Class<? extends ChannelHandler> handlerClass, Class<?>[] argTypes, Object... args) {
            handlerConstructors.add(() -> {
                try {
                    return handlerClass.getDeclaredConstructor(argTypes).newInstance(args);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            return this;
        }

        public NettyInitializer build() {
            return new NettyInitializer(this); 
        }
    }
}
