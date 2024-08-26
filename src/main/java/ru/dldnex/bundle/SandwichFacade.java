package ru.dldnex.bundle;

import io.netty.channel.Channel;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.Packet;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.exception.SandwichRuntimeException;
import ru.dldnex.bundle.network.PipelineAgent;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Yes.
 * Sandwiches could be used as a facade too.
 * I guess that explanation creates too many interpretations.
 */
public class SandwichFacade {

    private final IdentityFlagRegistry identityFlagRegistry = new IdentityFlagRegistry();
    private final PipelineAgent pipelineAgent;

    public SandwichFacade(@NotNull Logger logger) {
        pipelineAgent = new PipelineAgent(logger, this.identityFlagRegistry);
    }

    private @NotNull NetworkManager getNetworkManager(@NotNull Player player) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        return handle.playerConnection.networkManager;
    }

    public void inject(@NotNull Player player) throws SandwichRuntimeException {
        Channel channel = getNetworkManager(player).channel;
        pipelineAgent.instrument(channel);
    }

    public void sendPackets(@NotNull Player player, @NotNull Iterable<Object> packets) throws SandwichRuntimeException {
        NetworkManager networkManager = this.getNetworkManager(player);
        for (Object packet : packets) {
            networkManager.sendPacket((Packet<?>) packet);
        }
    }

    public void sendPackets(@NotNull Player player, @NotNull Object... packets) throws SandwichRuntimeException {
        this.sendPackets(player, Arrays.asList(packets));
    }

    public void sendSandwich(@NotNull Player player, @NotNull Iterable<Object> packets) throws SandwichRuntimeException {
        NetworkManager networkManager = this.getNetworkManager(player);
        Channel channel = networkManager.channel;
        // ProtocolLib using netty 4.0.23.Final. So method io.netty.util.AttributeMap.hasAttr() isn't implemented
        // in com.comphenix.protocol.injector.netty.channel.NettyChannelProxy
        if (channel.attr(PipelineAgent.MARKER).get() == null) {
            throw new SandwichRuntimeException("Sandwiched packets not sent. You probably forgot to call inject.");
        }
        Iterator<Object> iterator = packets.iterator();
        while (iterator.hasNext()) {
            Object packet = iterator.next();
            this.identityFlagRegistry.putFlag(packet, !iterator.hasNext());
            // FIXME: test that part. Network manager may break the ordering. Blame Minecraft devs.
            networkManager.sendPacket((Packet<?>) packet);
        }
    }

    public void sendSandwich(@NotNull Player player, @NotNull Object... packets) throws SandwichRuntimeException {
        this.sendSandwich(player, Arrays.asList(packets));
    }

}
