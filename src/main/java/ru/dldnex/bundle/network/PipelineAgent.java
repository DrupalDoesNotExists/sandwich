package ru.dldnex.bundle.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.AttributeKey;
import net.minecraft.server.v1_16_R3.Packet;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.exception.SandwichRuntimeException;
import ru.dldnex.bundle.network.handler.DebugHandler;
import ru.dldnex.bundle.network.handler.SandwichSqueezer;
import ru.dldnex.bundle.network.handler.proxy.ProxyEncoderByteBuf;
import ru.dldnex.bundle.network.handler.proxy.ProxyEncoderPacket;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;
import ru.dldnex.bundle.util.CollectionUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.*;
import java.util.logging.Logger;

public class PipelineAgent {
    private static final boolean PROXY_CHANNEL_HANDLERS = false;
    private static final boolean DEBUG_PIPELINE = false;
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

    public PipelineAgent(@NotNull Plugin plugin, @NotNull IdentityFlagRegistry flagRegistry) {
        this.plugin = plugin;
        this.flagRegistry = flagRegistry;
    }

    public void instrument(@NotNull Channel channel) throws SandwichRuntimeException {
        ChannelPipeline pipeline = channel.pipeline();

        // TODO: replace natives with proxies here!
        pipeline.addAfter("decoder", "sandwich", new SandwichSqueezer(this.flagRegistry));

        // Mark as injected
        channel.attr(MARKER).set(true);

        Logger logger = this.plugin.getLogger();

        Set<String> proxyHandlerNames = new HashSet<>();

        if (PROXY_CHANNEL_HANDLERS) {
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
                    ChannelHandler oldHandler = pipeline.remove(handlerName);
                    if (!(oldHandler instanceof MessageToByteEncoder<?> oldToByteEncoder)) {
                        logger.warning("Unable to proxy handler " + oldHandler.getClass().getName());
                        continue;
                    }
                    ChannelHandler newHandler = createHandlerProxy(oldToByteEncoder, isPacketsEncoder);
                    pipeline.addAfter(previousProxyHandlerName, handlerName, newHandler);

                    previousProxyHandlerName = handlerName;

                    proxyHandlerNames.add(handlerName);
                }
            }
        }

        if (DEBUG_PIPELINE) {
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

                logger.info("Pipeline with packets writing order (reversed pipeline.names()):");
                for (String handlerName : CollectionUtils.reverseList(pipeline.names())) {
                    if (debugHandlerNames.contains(handlerName)) continue;
                    this.plugin.getLogger().info((proxyHandlerNames.contains(handlerName) ? "[PROXY] " : "") + handlerName
                            + ": " + handlersHandlingTypesAfter.get(handlerName)
                            + " -> " + handlersHandlingTypesBefore.get(handlerName));
                }
            }, 20L * 3);
        }
    }

    @SuppressWarnings("unchecked")
    private static ChannelHandler createHandlerProxy(MessageToByteEncoder<?> oldToByteEncoder, boolean isPacketsEncoder) {
        return isPacketsEncoder
                ? new ProxyEncoderPacket((MessageToByteEncoder<Packet<?>>) oldToByteEncoder)
                : new ProxyEncoderByteBuf((MessageToByteEncoder<ByteBuf>) oldToByteEncoder);
    }
}
