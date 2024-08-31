package ru.dldnex.bundle.network.handler.proxy;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.network.NettyReflection;
import ru.dldnex.bundle.network.PipelineAgent;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.util.List;
import java.util.logging.Logger;

public class ProxyMessageToMessageEncoder extends MessageToMessageEncoder<Object> implements ProxyChannelHandler {
    private final @NotNull Logger logger;
    private final @NotNull IdentityFlagRegistry flagRegistry;
    private final @NotNull MessageToMessageEncoder<Object> proxied;

    public ProxyMessageToMessageEncoder(@NotNull Logger logger,
                                        @NotNull IdentityFlagRegistry flagRegistry,
                                        @NotNull MessageToMessageEncoder<Object> proxied
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
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) {
        NettyReflection.encode(this.proxied, ctx, msg, out);

        Boolean flag = this.flagRegistry.pullFlag(msg);
        if (flag == null) return;

        for (Object obj : out) {
            this.flagRegistry.putFlag(obj, false);
        }
        if (flag && !out.isEmpty()) {
            this.flagRegistry.putFlag(out.get(out.size() - 1), true);
        }

        if (PipelineAgent.DEBUG_PROXIES) this.logger.info("Updated flag for object " + msg.getClass().getName()
                + " in handler " + this.proxied.getClass().getName());
    }

    @Override
    public boolean acceptOutboundMessage(Object msg) throws Exception {
        return this.proxied.acceptOutboundMessage(msg);
    }
}
