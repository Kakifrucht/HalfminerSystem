package de.halfminer.hms.modules;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Respawn player at custom location
 */
@SuppressWarnings("unused")
public class ModRespawn extends HalfminerModule implements Listener {

    private Location respawnLoc;

    public ModRespawn() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {

        e.setRespawnLocation(respawnLoc);
    }

    public Location getSpawn() {
        return respawnLoc;
    }

    public void setSpawn(Location loc) {

        respawnLoc = loc;
        hms.getStorage().set("sys.spawnlocation", loc);
        loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public void reloadConfig() {

        Object loc = storage.get("sys.spawnlocation");

        if (loc instanceof Location) respawnLoc = (Location) loc;
        else respawnLoc = hms.getServer().getWorlds().get(0).getSpawnLocation();
    }
}
