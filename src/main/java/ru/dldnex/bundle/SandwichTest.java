package ru.dldnex.bundle;

import net.minecraft.server.v1_16_R3.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_16_R3.PacketPlayOutStopSound;
import net.minecraft.server.v1_16_R3.SoundCategory;
import net.minecraft.server.v1_16_R3.SoundEffects;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SandwichTest extends JavaPlugin implements Listener {
    private final SandwichFacade sandwichFacade = new SandwichFacade();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void joined(PlayerJoinEvent event) {
        sandwichFacade.inject(event.getPlayer());
        Location location = event.getPlayer().getLocation();
        sandwichFacade.sendSandwich(event.getPlayer(),
                new PacketPlayOutNamedSoundEffect(SoundEffects.ITEM_ARMOR_EQUIP_ELYTRA, SoundCategory.MASTER, location.getX(), location.getY(), location.getZ(), 10, 1),
                new PacketPlayOutStopSound()
        );
    }

}
