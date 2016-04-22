package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * - Counts players block breaks
 *   - Clears after no protected blocks were broken
 * - Set protected blocks via config
 * - Threshold ratio between broken blocks and broken protected blocks
 * - Notifies staff if threshold was passed
 *   - Shows last location
 *   - Notifies on join, if staff was offline
 */
@SuppressWarnings("unused")
public class ModAntiXray extends HalfminerModule implements Listener {

    private static int CHECK_THRESHOLD_SECONDS;
    private static int PROTECTED_BLOCK_THRESHOLD;
    private static int PROTECTED_BLOCK_RATIO;

    private Map<UUID, BreakCounter> playersChecked = new HashMap<>();
    private Set<UUID> checkedPermanently = new HashSet<>();
    private Set<Material> protectedMaterial;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {

        Player p = e.getPlayer();
        final UUID uuid = p.getUniqueId();
        BreakCounter counter = null;

        if (p.hasPermission("hms.bypass.antixray")) return;

        if (playersChecked.containsKey(uuid)) {
            counter = playersChecked.get(uuid);
            counter.incrementBreakages();
        }

        if (protectedMaterial.contains(e.getBlock().getType())) {

            int broken = 1;
            int brokenProtected = 1;

            if (counter == null) {
                counter = new BreakCounter(uuid);
                playersChecked.put(uuid, counter);
            }
            else {
                broken = counter.getBreakages();
                brokenProtected = counter.incrementProtectedBlocksBroken(e.getBlock().getLocation());
            }

            if (brokenProtected >= PROTECTED_BLOCK_THRESHOLD) {
                if (broken / brokenProtected > PROTECTED_BLOCK_RATIO) {
                    counter.setBypassScheduler();
                    checkedPermanently.add(uuid);
                    server.broadcast(Language.getMessagePlaceholders("modAntiXrayDetected", true,
                            "%PREFIX%", "AntiXRay", "%PLAYER%", p.getName(),
                            "%BROKENTOTAL%", String.valueOf(broken),
                            "%BROKENPROTECTED%", String.valueOf(brokenProtected)), "hms.antixray.notify");
                }
            }
        }
    }

    @EventHandler
    public void onLoginNotify(PlayerJoinEvent e) {

        Player joined = e.getPlayer();
        if (joined.hasPermission("hms.antixray.notify")) {
            for (UUID checked : checkedPermanently) {

                BreakCounter counter = playersChecked.get(checked);
                if (counter.isAlreadyInformed(joined)) continue;
                joined.sendMessage(Language.getMessagePlaceholders("modAntiXrayJoinDetected", true,
                        "%PREFIX%", "AntiXRay", "%PLAYER%", server.getPlayer(checked).getName(),
                        "%BROKENTOTAL%", String.valueOf(counter.getBreakages()),
                        "%BROKENPROTECTED%", String.valueOf(counter.getProtectedBreakages()),
                        "%LASTLOCATION%", Language.getStringFromLocation(counter.getLastProtectedLocation()),
                        "%WORLD%", counter.getLastProtectedLocation().getWorld().getName()));
                counter.setInformed(joined);
            }
        }
    }

    @Override
    public void loadConfig() {

        CHECK_THRESHOLD_SECONDS = hms.getConfig().getInt("antiXray.intervalUntilClearSeconds", 600);
        PROTECTED_BLOCK_THRESHOLD = hms.getConfig().getInt("antiXray.protectedBlockThreshold", 10);
        PROTECTED_BLOCK_RATIO = hms.getConfig().getInt("antiXray.protectedBlockRatioThreshold", 15);

        protectedMaterial = Utils.stringListToMaterialSet(hms.getConfig().getStringList("antiXray.protectedBlocks"));
    }

    private class BreakCounter {

        final UUID uuid;

        final Set<UUID> alreadyInformed = new HashSet<>();

        int blocksBroken = 1;
        int protectedBlocksBroken = 1;
        Location lastProtectedLocation;

        long lastBreakTime = System.currentTimeMillis() / 1000;
        boolean bypassScheduler = false;

        BukkitTask task;

        BreakCounter(final UUID uuid) {
            this.uuid = uuid;
            scheduleTask();
        }

        int getBreakages() {
            return blocksBroken;
        }

        int getProtectedBreakages() {
            return protectedBlocksBroken;
        }

        Location getLastProtectedLocation() {
            return lastProtectedLocation;
        }

        void incrementBreakages() {
            lastBreakTime = System.currentTimeMillis() / 1000;
            blocksBroken++;
        }

        int incrementProtectedBlocksBroken(Location loc) {
            lastProtectedLocation = loc;
            return protectedBlocksBroken++;
        }

        boolean isAlreadyInformed(Player p) {
            return alreadyInformed.contains(p.getUniqueId());
        }

        void setInformed(Player p) {
            alreadyInformed.add(p.getUniqueId());
        }

        void setBypassScheduler() {
            bypassScheduler = true;
        }

        void scheduleTask() {

            if (bypassScheduler) return;

            int schedulerTime = CHECK_THRESHOLD_SECONDS;

            long currentTime = System.currentTimeMillis() / 1000;
            if (lastBreakTime != currentTime) schedulerTime = schedulerTime - (int) (currentTime - lastBreakTime);

            if (task != null) task.cancel();
            task = scheduler.runTaskLater(hms, new Runnable() {
                @Override
                public void run() {

                    if (!bypassScheduler && lastBreakTime + CHECK_THRESHOLD_SECONDS <= System.currentTimeMillis() / 1000)
                        playersChecked.remove(uuid);
                    else scheduleTask();
                }
            }, schedulerTime * 20);
        }
    }
}
