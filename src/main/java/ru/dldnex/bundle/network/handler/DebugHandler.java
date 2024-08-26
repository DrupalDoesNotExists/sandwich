package ru.dldnex.bundle.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.server.v1_16_R3.Packet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DebugHandler extends ChannelOutboundHandlerAdapter {
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
