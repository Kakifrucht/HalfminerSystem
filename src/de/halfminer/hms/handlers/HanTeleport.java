package de.halfminer.hms.handlers;

import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.interfaces.Reloadable;
import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * - Disallows movement
 * - Change default time in config
 * - Show bossbar during teleport
 * - Stops when player taking damage
 * - Execute runnable after successful (or unsuccessful) teleport
 * - Only one teleport at a time
 */
@SuppressWarnings("SameParameterValue")
public class HanTeleport extends HalfminerHandler implements Reloadable {

    private final HanBossBar bar = (HanBossBar) hms.getHandler(HandlerType.BOSSBAR);

    private final Map<String, String> lang = new HashMap<>();
    private int defaultTime;

    private final Map<Player, BukkitTask> currentTeleport = new HashMap<>();

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

        player.sendMessage(Language.placeholderReplace(lang.get("start"), "%TIME%", String.valueOf(delay)));
        bar.sendBar(player, lang.get("startbar"), BarColor.YELLOW, BarStyle.SOLID, delay);
        currentTeleport.put(player, scheduler.runTaskTimer(hms, tp, 25L, 20L));
    }

    public boolean hasPendingTeleport(Player player, boolean tellPlayer) {

        boolean pending = currentTeleport.containsKey(player);
        if (tellPlayer && pending) player.sendMessage(lang.get("pending"));
        return pending;
    }

    @Override
    public void loadConfig() {

        defaultTime = hms.getConfig().getInt("handler.teleport.cooldownSeconds", 3);

        lang.put("start", Language.getMessagePlaceholders("hanTeleportStart", true, "%PREFIX%", "Teleport"));
        lang.put("startbar", Language.getMessage("hanTeleportBar"));
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

            if (toRun != null && teleportSuccessful) scheduler.runTaskLater(hms, toRun, 0L);
        }

        private void cancelTask(boolean teleportSuccessful) {

            if (currentTeleport.containsKey(player)) {
                currentTeleport.get(player).cancel();
                currentTeleport.remove(player);
            }

            if (!teleportSuccessful) {
                bar.removeBar(player);
                if (toRunIfCancelled != null) scheduler.runTaskLater(hms, toRunIfCancelled, 0L);
            }
        }
    }
}
