package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Respawn player at custom location
 * - First time join message
 * - Execute custom command on first join
 */
@SuppressWarnings("unused")
public class ModRespawn extends HalfminerModule implements Listener {

    private final Set<OfflinePlayer> toTeleport = new HashSet<>();
    private Location respawnLoc;

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

            scheduler.runTaskLater(hms, new Runnable() {
                @Override
                public void run() {

                    joined.teleport(respawnLoc);
                    String command = hms.getConfig().getString("respawn.firstJoinCommand", "");
                    if (command.length() > 0) {
                        server.dispatchCommand(server.getConsoleSender(),
                                Language.placeholderReplace(command, "%PLAYER%", joined.getName()));
                    }
                }
            }, 1L);
        } else if (toTeleport.contains(joined)) {
            scheduler.runTaskLater(hms, new Runnable() {
                @Override
                public void run() {
                    joined.teleport(respawnLoc);
                    joined.sendMessage(Language.getMessagePlaceholders("modRespawnForced", true, "%PREFIX%", "Spawn"));
                    toTeleport.remove(joined);
                }
            }, 1L);
        }

        e.setJoinMessage(message);
    }

    public Location getSpawn() {
        return respawnLoc;
    }

    public boolean teleportToSpawnOnJoin(OfflinePlayer p) {

        if (toTeleport.contains(p)) {
            toTeleport.remove(p);
            return false;
        } else {
            toTeleport.add(p);
            return true;
        }
    }

    public void setSpawn(Location loc) {

        respawnLoc = loc;
        storage.set("spawnlocation", loc);
        loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public void loadConfig() {

        Object loc = storage.get("spawnlocation");

        if (loc instanceof Location) respawnLoc = (Location) loc;
        else respawnLoc = server.getWorlds().get(0).getSpawnLocation();
    }
}
