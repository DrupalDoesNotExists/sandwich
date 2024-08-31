package ru.dldnex.bundle.network.handler.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import ru.dldnex.bundle.network.NettyReflection;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.network.PipelineAgent;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.util.logging.Logger;

public class ProxyMessageToByteEncoderByteBuf extends MessageToByteEncoder<ByteBuf> implements ProxyChannelHandler {
    private final @NotNull Logger logger;
    private final @NotNull IdentityFlagRegistry flagRegistry;
    private final @NotNull MessageToByteEncoder<ByteBuf> proxied;

    public ProxyMessageToByteEncoderByteBuf(@NotNull Logger logger,
                                            @NotNull IdentityFlagRegistry flagRegistry,
                                            @NotNull MessageToByteEncoder<ByteBuf> proxied
    ) {
        this.logger = logger;
        this.flagRegistry = flagRegistry;
        this.proxied = proxied;
    }

    @Override
    public @NotNull ChannelHandler getProxiedHandler() {
        return this.proxied;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        NettyReflection.encode(this.proxied, ctx, msg, out);

        Boolean flag = this.flagRegistry.pullFlag(msg);
        this.flagRegistry.putFlag(out, flag);
        if (flag != null) {
            if (PipelineAgent.DEBUG_PROXIES) this.logger.info("Updated flag for object " + msg.getClass().getName()
                    + " in handler " + this.proxied.getClass().getName());
        }
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return this.proxied.acceptOutboundMessage(msg);
    }
}
