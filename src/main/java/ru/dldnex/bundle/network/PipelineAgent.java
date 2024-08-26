package ru.dldnex.bundle.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import net.minecraft.server.v1_16_R3.Packet;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.exception.SandwichRuntimeException;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.logging.Logger;

public class PipelineAgent {
    private static final MethodType ENCODE_METHOD_TYPE = MethodType.methodType(void.class, ChannelHandlerContext.class, Object.class, ByteBuf.class);
    private static final MethodHandle METHOD_HANDLE;

    static {
        try {
            METHOD_HANDLE = MethodHandles.privateLookupIn(MessageToByteEncoder.class, MethodHandles.lookup())
                    .findVirtual(MessageToByteEncoder.class, "encode", ENCODE_METHOD_TYPE);
        } catch (Exception e) {
            throw new SandwichRuntimeException("Unable to prepare injection instruments", e);
        }
    }

    private final Plugin plugin;
    public static final AttributeKey<Boolean> MARKER = AttributeKey.valueOf("BadSandwich:Marker");
    private final @NotNull IdentityFlagRegistry flagRegistry;
    private final Map<String, Set<String>> handlersHandlingTypesBefore = new HashMap<>();
    private final Map<String, Set<String>> handlersHandlingTypesAfter = new HashMap<>();

    public PipelineAgent(@NotNull Plugin plugin, @NotNull IdentityFlagRegistry flagRegistry) {
        this.plugin = plugin;
        this.flagRegistry = flagRegistry;
    }

    public void instrument(@NotNull Channel channel) throws SandwichRuntimeException {
        ChannelPipeline pipeline = channel.pipeline();

        String beforeHandlerName = pipeline.get("encrypt") != null ? "encrypt" : "packet_handler";
        // TODO: replace natives with proxies here!
        pipeline.addBefore(beforeHandlerName, "sandwich", new SandwichSqueezer(this.flagRegistry));

        // Mark as injected
        channel.attr(MARKER).set(true);

        Logger logger = this.plugin.getLogger();

        List<String> handlerNamesNatural = pipeline.names();
        for (String name : pipeline.names()) {
            if (name.equals("DefaultChannelPipeline$TailContext#0")) {
                logger.info("Unable to enable debug for channel \"" + name + "\"");
                continue;
            }
            pipeline.addBefore(name, "debug-before-" + name, new DebugHandler(name, this.handlersHandlingTypesBefore));
            pipeline.addAfter(name, "debug-after-" + name, new DebugHandler(name, this.handlersHandlingTypesAfter));
        }

        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            logger.info("* PIPELINE (natural order - writing, debug handlers added with \"pipeline.addAfter()\"):");
            for (String handlerName : handlerNamesNatural) {
                plugin.getLogger().info(handlerName
                        + ": " + this.handlersHandlingTypesBefore.get(handlerName)
                        + " -> " + this.handlersHandlingTypesAfter.get(handlerName));
            }
        }, 20L * 3);
    }

    private class DebugHandler extends ChannelOutboundHandlerAdapter {
        private final String name;
        private final Map<String, Set<String>> writeClassNames;

        public DebugHandler(String name, Map<String, Set<String>> writeClassNames) {
            this.name = name;
            this.writeClassNames = writeClassNames;
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            String className = msg instanceof Packet<?> ? "<Packets>" : msg.getClass().getSimpleName();
            this.writeClassNames.computeIfAbsent(this.name, value -> new HashSet<>()).add(className);
            super.write(ctx, msg, promise);
        }
    }
}
