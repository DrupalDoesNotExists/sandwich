package ru.dldnex.bundle.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

/**
 * Collects the sandwiched messages until the flush flagged.
 */
public class SandwichSqueezer extends ChannelOutboundHandlerAdapter {
    private final IdentityFlagRegistry flagRegistry;

    public SandwichSqueezer(@NotNull IdentityFlagRegistry flagRegistry) {
        this.flagRegistry = flagRegistry;
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
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
