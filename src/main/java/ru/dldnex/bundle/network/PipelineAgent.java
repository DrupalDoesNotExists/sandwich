package ru.dldnex.bundle.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.exception.SandwichRuntimeException;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class PipelineAgent {
    private static final MethodType ENCODE_METHOD_TYPE = MethodType.methodType(void.class, ChannelHandlerContext.class, Object.class, ByteBuf.class);
    private static final MethodHandle METHOD_HANDLE;

    static {
        try {
            METHOD_HANDLE = MethodHandles.privateLookupIn(MessageToByteEncoder.class, MethodHandles.lookup())
                    .findVirtual(MessageToByteEncoder.class, "encode", ENCODE_METHOD_TYPE);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new SandwichRuntimeException("", e);
        }
    }

    public static final AttributeKey<Boolean> MARKER = AttributeKey.valueOf("BadSandwich:Marker");
    private final @NotNull IdentityFlagRegistry flagRegistry;

    public PipelineAgent(@NotNull IdentityFlagRegistry flagRegistry) {
        this.flagRegistry = flagRegistry;
    }

    public void instrument(@NotNull Channel channel) throws SandwichRuntimeException {
        ChannelPipeline pipeline = channel.pipeline();
        // TODO: replace natives with proxies here!
        pipeline.addBefore("encrypt", "sandwich", new SandwichSqueezer(this.flagRegistry));

        // Mark as injected
        channel.attr(MARKER).set(true);
    }
}
