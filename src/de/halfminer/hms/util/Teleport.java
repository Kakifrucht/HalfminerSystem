package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

public class Teleport {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    private BukkitTask task;

    private final int defaultTime;
    private final Player player;
    private final Location loc;

    private Runnable toRun = null;
    private Runnable toRunIfCancelled = null;

    /**
     * Create a new teleport.
     * The Player may not move, logout or get damage during the teleport.
     * @param player to teleport
     * @param loc location to teleport to
     */
    public Teleport(Player player, Location loc) {

        this.defaultTime = hms.getConfig().getInt("teleport.cooldownSeconds", 3);
        this.player = player;
        this.loc = loc;
    }

    /**
     * Start the teleport with in config given default delay.
     */
    public void startTeleport() {
        startTeleport(defaultTime);
    }

    /**
     * Start the teleport.
     * @param delay time in seconds player may not move
     */
    public void startTeleport(final int delay) {

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

                EntityDamageEvent lastDamageNow = player.getLastDamageCause();

                if (player.getLocation().getBlockX() != blockX
                        || player.getLocation().getBlockY() != blockY
                        || player.getLocation().getBlockZ() != blockZ
                        || !player.isOnline() || player.isDead()
                        || (lastDamageNow != null && !lastDamageNow.equals(lastDamage))) {

                    player.sendMessage(Language.getMessagePlaceholders("teleportMoved", true, "%PREFIX%", "Teleport"));
                    cancelTeleport(true);
                } else if (--seconds == 0) teleport();
            }
        }, 20L, 20L);
    }

    /**
     * Start the teleport with in config given default delay while
     * specifiying runnables that run if the teleport is successful or cancelled
     * @param toRun Runnable that will be executed if teleport successful, may be null
     * @param toRunIfCancelled Runnable that will be executed if teleport unsuccessful, may be null
     */
    public void startTeleportAndRun(Runnable toRun, Runnable toRunIfCancelled) {

        startTeleportAndRun(defaultTime, toRun, toRunIfCancelled);
    }

    /**
     * Start the teleport while specifiying runnables that run if the teleport is successful or cancelled
     * @param delay time in seconds player may not move
     * @param toRun Runnable that will be executed if teleport successful, may be null
     * @param toRunIfCancelled Runnable that will be executed if teleport unsuccessful, may be null
     */
    public void startTeleportAndRun(int delay, Runnable toRun, Runnable toRunIfCancelled) {

        this.toRun = toRun;
        this.toRunIfCancelled = toRunIfCancelled;
        startTeleport(delay);
    }

    private void teleport() {

        player.sendMessage(Language.getMessagePlaceholders("teleportDone", true, "%PREFIX%", "Teleport"));
        player.teleport(loc);
        cancelTeleport(false);

        if (toRun != null) hms.getServer().getScheduler().runTaskLater(hms, toRun, 1L);
    }

    private void cancelTeleport(boolean cancelled) {

        if (task != null) task.cancel();
        if (toRunIfCancelled != null && cancelled) hms.getServer().getScheduler().runTaskLater(hms, toRunIfCancelled, 1L);
    }
}
