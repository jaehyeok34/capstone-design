package capstone.design.netty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import capstone.design.Utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;

public class NettyInitializer extends ChannelInitializer<Channel> {

    private final List<Supplier<ChannelHandler>> handlerConstructors;

    private NettyInitializer(Builder builder) {
        this.handlerConstructors = builder.handlerConstructors;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        handlerConstructors.forEach(constructor -> ch.pipeline().addLast(constructor.get()));
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {

        private final List<Supplier<ChannelHandler>> handlerConstructors = new ArrayList<>();

        private Builder() {}

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
            Utils.validate(handlerConstructors);
            
            return new NettyInitializer(this); 
        }
    }
}
