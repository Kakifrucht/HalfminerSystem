package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Respawn player at custom location
 * - First time join message
 * - Execute custom command on first join
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

    @EventHandler
    public void onFirstJoin(final PlayerJoinEvent e) {

        String message = "";
        final Player joined = e.getPlayer();

        if (!joined.hasPlayedBefore()) {

            message = Language.getMessagePlaceholders("modRespawnFirstJoin", false, "%PLAYER%", joined.getName());

            hms.getServer().getScheduler().runTaskLater(hms, new Runnable() {
                @Override
                public void run() {

                    joined.teleport(respawnLoc);
                    String command = hms.getConfig().getString("respawn.firstJoinCommand", "");
                    if (command.length() > 0) {
                        hms.getServer().dispatchCommand(hms.getServer().getConsoleSender(),
                                Language.placeholderReplace(command, "%PLAYER%", joined.getName()));
                    }
                }
            }, 1L);
        }

        e.setJoinMessage(message);
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
