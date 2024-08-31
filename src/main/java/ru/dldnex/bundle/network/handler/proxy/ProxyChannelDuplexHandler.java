package ru.dldnex.bundle.network.handler.proxy;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.network.PipelineAgent;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.util.logging.Logger;

public class ProxyChannelDuplexHandler extends ChannelDuplexHandler implements ProxyChannelHandler {
    @SuppressWarnings("FieldCanBeLocal")
    private final @NotNull Logger logger;
    @SuppressWarnings("FieldCanBeLocal")
    private final @NotNull IdentityFlagRegistry flagRegistry;
    private final @NotNull ChannelDuplexHandler proxied;

    public ProxyChannelDuplexHandler(@NotNull Logger logger,
                                     @NotNull IdentityFlagRegistry flagRegistry,
                                     @NotNull ChannelDuplexHandler proxied
    ) {
        this.logger = logger;
        this.flagRegistry = flagRegistry;
        this.proxied = proxied;

        if (PipelineAgent.DEBUG_INJECTION) {
            this.logger.warning("Unable to proxy " + this.proxied.getClass().getName() + ". " +
                    this.getClass().getName() + " isn't implemented yet");
        }
    }

    @Override
    public @NotNull ChannelHandler getProxiedHandler() {
        return this.proxied;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        this.proxied.write(ctx, msg, promise);

        // TODO Implement flagRegistry logics using custom pipeline
    }
}
