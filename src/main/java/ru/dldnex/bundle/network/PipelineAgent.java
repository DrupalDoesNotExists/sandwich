package ru.dldnex.bundle.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.AttributeKey;
import net.minecraft.server.v1_16_R3.Packet;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.exception.SandwichRuntimeException;
import ru.dldnex.bundle.network.handler.DebugHandler;
import ru.dldnex.bundle.network.handler.SandwichSqueezer;
import ru.dldnex.bundle.network.handler.proxy.*;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;
import ru.dldnex.bundle.util.CollectionUtils;

import java.util.*;
import java.util.logging.Logger;

public class PipelineAgent {
    private static final boolean PROXY_CHANNEL_HANDLERS = true;
    private static final boolean LEGACY_INJECTION = true;
    public static final boolean DEBUG_INJECTION = false;
    private static final boolean DEBUG_PIPELINE = false;
    public static final boolean DEBUG_PROXIES = false;

    private final Plugin plugin;
    private final Logger logger;
    public static final AttributeKey<Boolean> MARKER = AttributeKey.valueOf("BadSandwich:Marker");
    private final @NotNull IdentityFlagRegistry flagRegistry;

    public PipelineAgent(@NotNull Plugin plugin, @NotNull IdentityFlagRegistry flagRegistry) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.flagRegistry = flagRegistry;
    }

    public void instrument(@NotNull Channel channel) throws SandwichRuntimeException {
        if (channel.attr(MARKER).get() != null) return; // Already patched
        channel.attr(MARKER).set(true); // Mark as patched

        this.patchPipeline(channel);

        if (DEBUG_PIPELINE) {
            this.debugPipeline(channel, 3);
        }
    }

    private void patchPipeline(@NotNull Channel channel) {
        ChannelPipeline pipeline = channel.pipeline();

        pipeline.addAfter("decoder", "sandwich", new SandwichSqueezer(this.flagRegistry));

        if (!PROXY_CHANNEL_HANDLERS) return;

        String previousProxyHandlerName = null;
        boolean isPacketsEncoder = false;
        List<String> handlerNames = new ArrayList<>(pipeline.names());
        for (String handlerName : handlerNames) {
            if (handlerName.equals("encoder")) {
                isPacketsEncoder = true;
            }
            if (handlerName.equals("sandwich")) {
                previousProxyHandlerName = handlerName;
            } else if (handlerName.equals("packet_handler")) {
                previousProxyHandlerName = null;
            } else if (previousProxyHandlerName != null) {
                ChannelHandler oldHandler = pipeline.get(handlerName);
                if (oldHandler == null) {
                    throw new IllegalStateException("Old handler not found");
                }
                ChannelHandler newHandler = createHandlerProxy(oldHandler, isPacketsEncoder);
                if (newHandler != null) {
                    pipeline.remove(handlerName);
                    pipeline.addAfter(previousProxyHandlerName, handlerName, newHandler);

                    if (DEBUG_INJECTION) {
                        this.logger.info("Created " + newHandler.getClass().getSimpleName()
                                + " for " + oldHandler.getClass().getName());
                    }
                }

                previousProxyHandlerName = handlerName;
            }
        }
    }

    private void debugPipeline(@NotNull Channel channel, int dataStoreDurationSeconds) {
        ChannelPipeline pipeline = channel.pipeline();

        Map<String, Set<String>> handlersHandlingTypesBefore = new HashMap<>();
        Map<String, Set<String>> handlersHandlingTypesAfter = new HashMap<>();

        List<String> debugHandlerNames = new ArrayList<>();
        for (String name : pipeline.names()) {
            if (name.equals("DefaultChannelPipeline$TailContext#0")) continue; // channel with that name not found
            pipeline.addBefore(name, "debug-before-" + name, new DebugHandler(name, handlersHandlingTypesBefore));
            debugHandlerNames.add("debug-before-" + name);
            pipeline.addAfter(name, "debug-after-" + name, new DebugHandler(name, handlersHandlingTypesAfter));
            debugHandlerNames.add("debug-after-" + name);
        }

        this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, () -> {
            if (!channel.isOpen()) return;

            for (String debugHandlerName : debugHandlerNames) {
                pipeline.remove(debugHandlerName);
            }

            this.logger.info("Pipeline with packets writing order (reversed pipeline.names()):");
            for (String handlerName : CollectionUtils.reverseList(pipeline.names())) {
                if (debugHandlerNames.contains(handlerName)) continue;
                boolean proxied = false;
                ChannelHandler handler = pipeline.get(handlerName);
                if (handler instanceof ProxyChannelHandler proxyHandler) {
                    proxied = true;
                    handler = proxyHandler.getProxiedHandler();
                }
                this.plugin.getLogger().info(
                        (handler == null ? "handler.not.found"
                                : handler.getClass().getName()
                                + (proxied ? " (proxied with " + handler.getClass().getSimpleName() + ")" : ""))
                                + " | " + handlerName
                                + ": " + handlersHandlingTypesAfter.get(handlerName)
                                + " -> " + handlersHandlingTypesBefore.get(handlerName));
            }
        }, dataStoreDurationSeconds * 20L);
    }

    @SuppressWarnings("unchecked")
    private ChannelHandler createHandlerProxy(ChannelHandler handler, boolean isPacketsEncoder) {
        if (!(handler instanceof ChannelOutboundHandler)) return null;

        if (LEGACY_INJECTION) {
            if (handler instanceof MessageToByteEncoder<?> specificHandler) {
                return isPacketsEncoder
                        ? new ProxyMessageToByteEncoderPacket(this.logger, this.flagRegistry,
                        (MessageToByteEncoder<Packet<?>>) specificHandler)
                        : new ProxyMessageToByteEncoderByteBuf(this.logger, this.flagRegistry,
                        (MessageToByteEncoder<ByteBuf>) specificHandler);
            } else if (handler instanceof MessageToMessageEncoder<?> specificHandler) {
                return isPacketsEncoder
                        ? new ProxyMessageToMessageEncoderPacket(this.logger, this.flagRegistry,
                        (MessageToMessageEncoder<Packet<?>>) specificHandler)
                        : new ProxyMessageToMessageEncoderByteBuf(this.logger, this.flagRegistry,
                        (MessageToMessageEncoder<ByteBuf>) specificHandler);
            } else if (handler instanceof ChannelDuplexHandler specificHandler) {
                return new ProxyChannelDuplexHandler(this.logger, this.flagRegistry,
                        specificHandler);
            }
        } else {
            if (handler instanceof MessageToByteEncoder<?> specificHandler) {
                return new ProxyMessageToByteEncoder(this.logger, this.flagRegistry,
                        (MessageToByteEncoder<Object>) specificHandler);
            } else if (handler instanceof MessageToMessageEncoder<?> specificHandler) {
                return new ProxyMessageToMessageEncoder(this.logger, this.flagRegistry,
                        (MessageToMessageEncoder<Object>) specificHandler);
            } else if (handler instanceof ChannelDuplexHandler specificHandler) {
                return new ProxyChannelDuplexHandler(this.logger, this.flagRegistry,
                        specificHandler);
            }
        }

        if (DEBUG_INJECTION) {
            this.logger.warning("Unable to create proxy of handler " + handler.getClass().getName());
        }

        return null;
    }
}
