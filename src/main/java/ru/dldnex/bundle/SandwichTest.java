package ru.dldnex.bundle;

import net.minecraft.server.v1_16_R3.*;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public class SandwichTest extends JavaPlugin implements Listener {

    private static final boolean USE_SANDWICH = true;

    private final SandwichFacade sandwichFacade = new SandwichFacade(this);
    private final Set<Player> watchingPlayers = new HashSet<>();
    private BukkitTask bukkitTask;

    @Override
    public void onEnable() {
        Server server = this.getServer();
        server.getPluginManager().registerEvents(this, this);
        this.bukkitTask = server.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (Player player : this.watchingPlayers) {
                this.sendTestSandwich(player);
            }
        }, 1L, 1L);
    }

    @Override
    public void onDisable() {
        this.bukkitTask.cancel();
        HandlerList.unregisterAll((Plugin) this);
    }

    @EventHandler
    public void joined(PlayerJoinEvent event) {
        if (USE_SANDWICH) sandwichFacade.inject(event.getPlayer());
    }

    @EventHandler
    public void joined(PlayerQuitEvent event) {
        watchingPlayers.remove(event.getPlayer());
    }

    @EventHandler
    public void joined(PlayerMoveEvent event) {
        if (!this.watchingPlayers.contains(event.getPlayer())) return;
        if (!event.hasChangedPosition()) return;
        if (event.getPlayer().isFlying()) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getY() == to.getY()) {
            event.getPlayer().sendMessage("Y not changed: " + from + " -> " + to);
            return;
        }
        if (from.getX() != to.getX()) {
            event.getPlayer().sendMessage("X changed: " + from.getX() + " -> " + to.getX());
            return;
        }
        if (from.getZ() != to.getZ()) {
            event.getPlayer().sendMessage("Z changed: " + from.getZ() + " -> " + to.getZ());
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage("Y changed: " + from.getY() + " -> " + to.getY());
    }

    @EventHandler
    public void joined(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().equalsIgnoreCase("/sandwich")) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission("sandwich.test")) {
            player.sendMessage("No enough permissions: " + "sandwich.test");
            return;
        }
        if (!this.watchingPlayers.remove(player)) {
            this.watchingPlayers.add(player);
            player.sendMessage("Debug enabled");
        } else {
            player.sendMessage("Debug disabled");
        }
    }

    private void sendTestSandwich(@NotNull Player player) {
        Location location = player.getLocation();

        Object[] packets;
        if (true) {
            Block block = location.getBlock().getRelative(BlockFace.DOWN);
            BlockPosition pos = new BlockPosition(block.getX(), block.getY(), block.getZ());
            packets = new Object[]{
                    new PacketPlayOutBlockChange(pos, Blocks.AIR.getBlockData()),
                    new PacketPlayOutBlockChange(pos, Blocks.STONE.getBlockData())
            };
        } else {
            packets = new Object[]{
                    new PacketPlayOutNamedSoundEffect(
                            SoundEffects.ITEM_ARMOR_EQUIP_ELYTRA, SoundCategory.MASTER,
                            location.getX(), location.getY(), location.getZ(),
                            10, 1
                    ),
                    new PacketPlayOutStopSound()
            };
        }

        if (USE_SANDWICH) {
            sandwichFacade.sendSandwich(player, packets);
        } else {
            sandwichFacade.sendPackets(player, packets);
        }
    }
}
