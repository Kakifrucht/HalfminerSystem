package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
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
 * - Threshold Y level until counted
 * - Notifies staff if threshold was passed
 *   - Shows last location
 *   - Notifies on join, if staff was offline
 */
public class ModAntiXray extends HalfminerModule implements Listener {

    private int timeUntilClear;
    private int protectedBlockThreshold;
    private int yLevelThreshold;
    private double protectedBlockRatio;

    private final Map<UUID, BreakCounter> playersChecked = new HashMap<>();
    private final Set<UUID> checkedPermanently = new HashSet<>();
    private Set<Material> protectedMaterial;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {

        Player p = e.getPlayer();
        if (p.hasPermission("hms.bypass.antixray")) return;

        Block brokenBlock = e.getBlock();
        if (brokenBlock.getLocation().getBlockY() > yLevelThreshold) return;

        UUID uuid = p.getUniqueId();
        BreakCounter counter = null;
        int blocksBroken = 1;

        if (playersChecked.containsKey(uuid)) {
            counter = playersChecked.get(uuid);
            if (counter.isBypassed()) return;
            blocksBroken = counter.incrementBreakages();
        }

        if (protectedMaterial.contains(brokenBlock.getType())) {

            if (counter == null) {
                counter = new BreakCounter(uuid);
                playersChecked.put(uuid, counter);
            }

            int brokenProtected = counter.incrementProtectedBlocksBroken(brokenBlock.getLocation());

            // Put player into permanent check mode
            if (brokenProtected >= protectedBlockThreshold
                    && brokenProtected / (double) blocksBroken > protectedBlockRatio) {

                boolean firstDetection = false;
                if (!counter.isCheckedPermanently()) {
                    firstDetection = true;
                    checkedPermanently.add(uuid);
                }

                // Notify if the bypass has only been set now or if the distance between the last ore is high enough
                if (firstDetection || counter.notifyAgain()) {

                    notify(server.getConsoleSender(), counter, false);
                    for (Player toNotify : server.getOnlinePlayers())
                        if (toNotify.hasPermission("hms.antixray.notify")) notify(toNotify, counter, false);
                }
            }
        }
    }

    @EventHandler
    public void onLoginNotify(PlayerJoinEvent e) {

        Player joined = e.getPlayer();
        if (joined.hasPermission("hms.antixray.notify"))
            for (UUID checked : checkedPermanently) {
                BreakCounter counter = playersChecked.get(checked);
                if (!counter.isBypassed()) notify(joined, counter, true);
            }
    }

    public boolean setBypassed(OfflinePlayer p) {

        BreakCounter counter = playersChecked.get(p.getUniqueId());

        if (counter == null) {
            counter = new BreakCounter(p.getUniqueId());
            playersChecked.put(p.getUniqueId(), counter);
        }

        return counter.toggleBypass();
    }

    public String getInformationString() {

        String toReturn = "";

        for (UUID uuid : playersChecked.keySet()) {

            BreakCounter counter = playersChecked.get(uuid);
            Location last = counter.getLastProtectedLocation();

            if (last == null) continue;

            ChatColor color = ChatColor.GRAY;
            if (counter.isBypassed()) color = ChatColor.YELLOW;
            else if (counter.isCheckedPermanently()) color = ChatColor.RED;

            toReturn += Language.getMessagePlaceholders("modAntiXrayShowFormat", false, "%PLAYER%",
                    color + counter.getOwnerName(), "%LOCATION%", Language.getStringFromLocation(last), "%WORLD%",
                    last.getWorld().getName(), "%PROTECTED%", String.valueOf(counter.getProtectedBreakages()),
                    "%BROKEN%", String.valueOf(counter.getBreakages())) + "\n";
        }

        if (toReturn.length() > 0) toReturn = toReturn.substring(0, toReturn.length() - 1);
        return toReturn;
    }

    private void notify(CommandSender toNotify, BreakCounter counter, boolean checkIfAlreadyNotified) {

        if (toNotify instanceof Player) {
            if (checkIfAlreadyNotified && counter.isAlreadyInformed((Player) toNotify)) return;
            else counter.setInformed((Player) toNotify);
        }

        toNotify.sendMessage(Language.getMessagePlaceholders("modAntiXrayJoinDetected", true,
                "%PREFIX%", "AntiXRay", "%PLAYER%", counter.getOwnerName(),
                "%BROKENTOTAL%", String.valueOf(counter.getBreakages()),
                "%BROKENPROTECTED%", String.valueOf(counter.getProtectedBreakages()),
                "%LASTLOCATION%", Language.getStringFromLocation(counter.getLastProtectedLocation()),
                "%WORLD%", counter.getLastProtectedLocation().getWorld().getName()));
    }

    @Override
    public void loadConfig() {

        timeUntilClear = hms.getConfig().getInt("antiXray.intervalUntilClearSeconds", 300);
        protectedBlockThreshold = hms.getConfig().getInt("antiXray.protectedBlockThreshold", 20);
        yLevelThreshold = hms.getConfig().getInt("antiXray.yLevelThreshold", 30);
        protectedBlockRatio = hms.getConfig().getDouble("antiXray.protectedBlockRatioThreshold", 0.04);

        protectedMaterial = Utils.stringListToMaterialSet(hms.getConfig().getStringList("antiXray.protectedBlocks"));
    }

    private class BreakCounter {

        final UUID uuid;

        final Set<UUID> alreadyInformed = new HashSet<>();

        int blocksBroken = 1;
        int protectedBlocksBroken = 0;
        Location pastProtectedLocation;
        Location lastProtectedLocation;

        long lastProtectedBreakTime = System.currentTimeMillis() / 1000;
        boolean bypass = false;

        BukkitTask task;

        BreakCounter(final UUID uuid) {
            this.uuid = uuid;
            scheduleTask();
        }

        String getOwnerName() {
            return storage.getPlayer(uuid).getName();
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

        boolean notifyAgain() {
            return pastProtectedLocation == null || pastProtectedLocation.distance(lastProtectedLocation) > 3.0d;
        }

        int incrementBreakages() {
            return ++blocksBroken;
        }

        int incrementProtectedBlocksBroken(Location loc) {

            lastProtectedBreakTime = System.currentTimeMillis() / 1000;
            pastProtectedLocation = lastProtectedLocation;
            lastProtectedLocation = loc;
            return ++protectedBlocksBroken;
        }

        boolean isAlreadyInformed(Player p) {
            return alreadyInformed.contains(p.getUniqueId());
        }

        void setInformed(Player p) {
            alreadyInformed.add(p.getUniqueId());
        }

        boolean toggleBypass() {
            return bypass = !bypass;
        }

        boolean isBypassed() {
            return bypass;
        }

        boolean isCheckedPermanently() {
            return checkedPermanently.contains(uuid);
        }

        void scheduleTask() {

            if (isCheckedPermanently()) return;

            int schedulerTime = timeUntilClear;

            long currentTime = System.currentTimeMillis() / 1000;
            if (lastProtectedBreakTime != currentTime)
                schedulerTime = schedulerTime - (int) (currentTime - lastProtectedBreakTime);

            if (task != null) task.cancel();
            task = scheduler.runTaskLater(hms, new Runnable() {
                @Override
                public void run() {

                    if (lastProtectedBreakTime + timeUntilClear <= System.currentTimeMillis() / 1000) {
                        if (!isCheckedPermanently()) playersChecked.remove(uuid);
                    }
                    else scheduleTask();
                }
            }, schedulerTime * 20);
        }
    }
}