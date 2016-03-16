package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class Teleport {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    private BukkitTask task;

    private Player player;
    private Location loc;

    public Teleport(Player player, Location loc) {
        startTeleport(player, loc, hms.getConfig().getInt("teleport.cooldownSeconds", 3));
    }

    public Teleport(Player player, Location loc, int delay) {
        startTeleport(player, loc, delay);
    }

    private void startTeleport(Player p, Location l, final int delay) {

        this.player = p;
        this.loc = l;

        if (delay < 1 || player.hasPermission("hms.bypass.teleporttimer")) {
            teleport();
            return;
        }

        final int blockX = player.getLocation().getBlockX();
        final int blockY = player.getLocation().getBlockY();
        final int blockZ = player.getLocation().getBlockZ();

        player.sendMessage(Language.getMessagePlaceholders("teleportStart", true,
                "%PREFIX%", "Teleport", "%TIME%", String.valueOf(delay)));

        task = hms.getServer().getScheduler().runTaskTimer(hms, new Runnable() {

            private int seconds = delay;
            @Override
            public void run() {

                if (player.getLocation().getBlockX() != blockX
                        || player.getLocation().getBlockY() != blockY
                        || player.getLocation().getBlockZ() != blockZ) {

                    player.sendMessage(Language.getMessagePlaceholders("teleportMoved", true, "%PREFIX%", "Teleport"));
                    cancelTeleport();
                } else if (--seconds == 0) teleport();
            }
        }, 20L, 20L);
    }

    private void teleport() {
        player.sendMessage(Language.getMessagePlaceholders("teleportDone", true, "%PREFIX%", "Teleport"));
        player.teleport(loc);
        cancelTeleport();
    }

    private void cancelTeleport() {
        if (task != null) task.cancel();
    }
}
