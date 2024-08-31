package ru.dldnex.bundle.network.handler.proxy;

import io.netty.channel.ChannelHandler;
import org.jetbrains.annotations.NotNull;

public interface ProxyChannelHandler {
    @NotNull ChannelHandler getProxiedHandler();
}
