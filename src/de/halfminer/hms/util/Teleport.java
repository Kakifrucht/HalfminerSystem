package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

public class Teleport {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    private BukkitTask task;

    private final Player player;
    private final Location loc;
    private final int delay;

    private Runnable toRun = null;

    public Teleport(Player player, Location loc) {
        this.player = player;
        this.loc = loc;
        this.delay = hms.getConfig().getInt("teleport.cooldownSeconds", 3);
    }

    public Teleport(Player player, Location loc, int delay) {
        this.player = player;
        this.loc = loc;
        this.delay = delay;
    }

    public void startTeleport() {

        if (delay < 1 || player.hasPermission("hms.bypass.teleporttimer")) {
            teleport();
            return;
        }

        final int blockX = player.getLocation().getBlockX();
        final int blockY = player.getLocation().getBlockY();
        final int blockZ = player.getLocation().getBlockZ();
        final EntityDamageEvent lastDamage = player.getLastDamageCause();

        player.sendMessage(Language.getMessagePlaceholders("teleportStart", true,
                "%PREFIX%", "Teleport", "%TIME%", String.valueOf(delay)));

        task = hms.getServer().getScheduler().runTaskTimer(hms, new Runnable() {

            private int seconds = delay;
            @Override
            public void run() {

                if (player.getLocation().getBlockX() != blockX
                        || player.getLocation().getBlockY() != blockY
                        || player.getLocation().getBlockZ() != blockZ
                        || !player.getLastDamageCause().equals(lastDamage)) {

                    player.sendMessage(Language.getMessagePlaceholders("teleportMoved", true, "%PREFIX%", "Teleport"));
                    cancelTeleport();
                } else if (--seconds == 0) teleport();
            }
        }, 20L, 20L);
    }

    public void startTeleportAndRun(Runnable toRun) {

        this.toRun = toRun;
        startTeleport();
    }

    private void teleport() {
        player.sendMessage(Language.getMessagePlaceholders("teleportDone", true, "%PREFIX%", "Teleport"));
        player.teleport(loc);
        cancelTeleport();

        if (toRun != null) hms.getServer().getScheduler().runTaskLater(hms, toRun, 1L);
    }

    private void cancelTeleport() {
        if (task != null) task.cancel();
    }
}
