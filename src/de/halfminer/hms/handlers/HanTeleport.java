package de.halfminer.hms.handlers;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler to do timed teleports.
 * - Disallows movement
 * - Change default time in config
 * - Stops when player taking damage
 * - Execute runnable after successful (or unsuccessful) teleport
 * - Only one teleport at a time
 */
public class HanTeleport extends HalfminerHandler {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    private final Map<String, String> lang = new HashMap<>();
    private int defaultTime;

    private final Map<Player, Integer> currentTeleport = new HashMap<>();

    @SuppressWarnings("unused")
    public HanTeleport() {
        reloadConfig();
    }

    public void startTeleport(Player player, Location loc) {
        startTeleport(player, loc, defaultTime, null, null);
    }

    public void startTeleport(Player player, Location loc, int delay) {
        startTeleport(player, loc, delay, null, null);
    }

    public void startTeleport(final Player player, final Location loc,
                              final int delay, Runnable toRun, Runnable toRunIfCancelled) {

        if (currentTeleport.containsKey(player)) {
            player.sendMessage(lang.get("pending"));
            return;
        }

        Teleport tp = new Teleport(player, loc, delay, toRun, toRunIfCancelled);

        if (delay < 1 || player.hasPermission("hms.bypass.teleporttimer")) {
            tp.teleport();
            return;
        }

        player.sendMessage(Language.placeholderReplace(lang.get("start"), "%TIME%", String.valueOf(delay)) );
        currentTeleport.put(player, hms.getServer().getScheduler().runTaskTimer(hms, tp, 20L, 20L).getTaskId());
    }

    public boolean hasPendingTeleport(Player player, boolean tellPlayer) {

        boolean pending = currentTeleport.containsKey(player);
        if (tellPlayer && pending) player.sendMessage(lang.get("pending"));
        return pending;
    }

    @Override
    public void reloadConfig() {

        defaultTime = hms.getConfig().getInt("teleport.cooldownSeconds", 3);

        lang.put("start", Language.getMessagePlaceholders("hanTeleportStart", true, "%PREFIX%", "Teleport"));
        lang.put("pending", Language.getMessagePlaceholders("hanTeleportPending", true, "%PREFIX%", "Teleport"));
        lang.put("moved", Language.getMessagePlaceholders("hanTeleportMoved", true, "%PREFIX%", "Teleport"));
        lang.put("done", Language.getMessagePlaceholders("hanTeleportDone", true, "%PREFIX%", "Teleport"));
    }

    private class Teleport implements Runnable {

        final private Player player;
        final private Location location;
        private int seconds;

        final private Runnable toRun;
        final private Runnable toRunIfCancelled;

        final EntityDamageEvent lastDamage;
        final private int originalX;
        final private int originalY;
        final private int originalZ;

        Teleport(Player player, Location loc, int delay, Runnable toRun, Runnable toRunIfCancelled) {

            this.player = player;
            this.location = loc;
            this.seconds = delay;

            this.toRun = toRun;
            this.toRunIfCancelled = toRunIfCancelled;

            lastDamage = player.getLastDamageCause();
            originalX = player.getLocation().getBlockX();
            originalY = player.getLocation().getBlockY();
            originalZ = player.getLocation().getBlockZ();
        }

        @Override
        public void run() {

            EntityDamageEvent lastDamageNow = player.getLastDamageCause();

            if (player.getLocation().getBlockX() != originalX
                    || player.getLocation().getBlockY() != originalY
                    || player.getLocation().getBlockZ() != originalZ
                    || !player.isOnline() || player.isDead()
                    || (lastDamageNow != null && !lastDamageNow.equals(lastDamage))) {

                player.sendMessage(lang.get("moved"));
                cancelTask(false);
            } else if (--seconds == 0) teleport();
        }

        private void teleport() {

            player.sendMessage(lang.get("done"));
            player.setFallDistance(0);
            boolean teleportSuccessful = player.teleport(location);
            cancelTask(teleportSuccessful);

            if (toRun != null && teleportSuccessful) hms.getServer().getScheduler().runTaskLater(hms, toRun, 1L);
        }

        private void cancelTask(boolean teleportSuccessful) {

            if (currentTeleport.containsKey(player)) {
                hms.getServer().getScheduler().cancelTask(currentTeleport.get(player));
                currentTeleport.remove(player);
            }

            if (toRunIfCancelled != null && !teleportSuccessful)
                hms.getServer().getScheduler().runTaskLater(hms, toRunIfCancelled, 1L);
        }
    }
}