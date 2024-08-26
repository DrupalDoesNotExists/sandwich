package ru.dldnex.bundle.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.util.logging.Logger;

/**
 * Collects the sandwiched messages until the flush flagged.
 */
public class SandwichSqueezer extends ChannelOutboundHandlerAdapter {
    private final Logger logger;
    private final IdentityFlagRegistry flagRegistry;
    private boolean usageDetected = false;

    public SandwichSqueezer(@NotNull Logger logger, @NotNull IdentityFlagRegistry flagRegistry) {
        this.logger = logger;
        this.flagRegistry = flagRegistry;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        if (!this.usageDetected) {
            this.usageDetected = true;
            this.logger.warning("Usage of " + this.getClass().getSimpleName() + " detected");
        }
        Boolean flag = this.flagRegistry.pullFlag(msg);
        // This packet is not a sandwich part
        if (flag == null) {
            // Double flush for unexpected situations handling
            ctx.flush().writeAndFlush(msg, promise);
            return;
        }

        // Write the packet data
        ctx.write(msg, promise);
        if (flag) {
            // And flush if FLUSH flag is set
            ctx.flush();
        }
    }
}
