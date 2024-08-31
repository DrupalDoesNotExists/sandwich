package ru.dldnex.bundle;

import io.netty.channel.Channel;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.NetworkManager;
import net.minecraft.server.v1_16_R3.Packet;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import ru.dldnex.bundle.exception.SandwichRuntimeException;
import ru.dldnex.bundle.network.PipelineAgent;
import ru.dldnex.bundle.registry.IdentityFlagRegistry;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Yes.
 * Sandwiches could be used as a facade too.
 * I guess that explanation creates too many interpretations.
 */
public class SandwichFacade {

    private final IdentityFlagRegistry identityFlagRegistry = new IdentityFlagRegistry();
    private final PipelineAgent pipelineAgent;

    public SandwichFacade(@NotNull Plugin plugin) {
        pipelineAgent = new PipelineAgent(plugin, this.identityFlagRegistry);
    }

    private @NotNull NetworkManager getNetworkManager(@NotNull Player player) {
        EntityPlayer handle = ((CraftPlayer) player).getHandle();
        return handle.playerConnection.networkManager;
    }

    @Deprecated
    public void sendPackets(@NotNull Player player, @NotNull Iterable<Object> packets) throws SandwichRuntimeException {
        NetworkManager networkManager = this.getNetworkManager(player);
        for (Object packet : packets) {
            networkManager.sendPacket((Packet<?>) packet);
        }
    }

    @Deprecated
    public void sendPackets(@NotNull Player player, @NotNull Object... packets) throws SandwichRuntimeException {
        this.sendPackets(player, Arrays.asList(packets));
    }

    public void sendSandwich(@NotNull Player player, @NotNull Iterable<Object> packets) throws SandwichRuntimeException {
        NetworkManager networkManager = this.getNetworkManager(player);
        Channel channel = networkManager.channel;

        this.pipelineAgent.instrument(channel); // inject to pipeline

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
