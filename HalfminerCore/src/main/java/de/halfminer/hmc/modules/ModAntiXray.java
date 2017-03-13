package de.halfminer.hmc.modules;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
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

import javax.annotation.Nullable;
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
public class ModAntiXray extends HalfminerModule implements Listener, Sweepable {

    private int timeUntilClear;
    private int protectedBlockThreshold;
    private int yLevelThreshold;
    private double protectedBlockRatio;

    private final Map<UUID, BreakCounter> observedPlayers = new HashMap<>();
    private final Set<UUID> observedPermanently = new HashSet<>();
    private Set<Material> protectedMaterial;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {

        Player p = e.getPlayer();
        if (p.hasPermission("hmc.bypass.antixray")) return;

        Block brokenBlock = e.getBlock();
        if (brokenBlock.getLocation().getBlockY() > yLevelThreshold) return;

        UUID uuid = p.getUniqueId();
        BreakCounter counter = null;
        int blocksBroken = 1;

        if (counterExists(uuid)) {
            counter = observedPlayers.get(uuid);
            blocksBroken = counter.incrementBreakages();
        }

        if (protectedMaterial.contains(brokenBlock.getType())) {

            if (counter == null) observedPlayers.put(uuid, counter = new BreakCounter(uuid));

            int brokenProtected = counter.incrementProtectedBlocksBroken(brokenBlock);

            // Put player into permanent check mode
            if (brokenProtected >= protectedBlockThreshold
                    && brokenProtected / (double) blocksBroken > protectedBlockRatio) {

                boolean firstDetection = !counter.isCheckedPermanently();
                if (firstDetection) observedPermanently.add(uuid);

                // Notify if the bypass has only been set now or if the distance between the last ore is high enough
                if (firstDetection || counter.notifyAgain()) {
                    notify(null, counter, false);
                }
            } else if (observedPermanently.contains(uuid)) observedPermanently.remove(uuid);
        }
    }

    @EventHandler
    public void onLoginNotify(PlayerJoinEvent e) {

        Player joined = e.getPlayer();
        if (joined.hasPermission("hmc.antixray.notify"))
            for (UUID checked : observedPermanently)
                notify(joined, observedPlayers.get(checked), true);
    }

    public boolean setBypassed(OfflinePlayer p) {

        UUID uuid = p.getUniqueId();

        if (counterExists(uuid)) {
            return observedPlayers.get(uuid).toggleBypass();
        } else {
            BreakCounter counter = new BreakCounter(uuid);
            observedPlayers.put(p.getUniqueId(), counter);
            return counter.toggleBypass();
        }
    }

    public String getInformationString() {

        String toReturn = "";

        // cleanup junk
        sweep();
        for (UUID uuid : observedPlayers.keySet()) {

            BreakCounter counter = observedPlayers.get(uuid);
            Location last = counter.getLastProtectedLocation();

            if (last == null) continue;

            ChatColor color = ChatColor.GRAY;
            if (counter.isBypassed()) color = ChatColor.YELLOW;
            else if (counter.isCheckedPermanently()) color = ChatColor.RED;

            toReturn += MessageBuilder.create("modAntiXrayShowFormat", hmc)
                    .addPlaceholderReplace("%PLAYER%", color + counter.getOwnerName())
                    .addPlaceholderReplace("%LOCATION%", Utils.getStringFromLocation(last))
                    .addPlaceholderReplace("%WORLD%", last.getWorld().getName())
                    .addPlaceholderReplace("%MATERIAL%", Utils.makeStringFriendly(counter.getLastMaterial().toString()))
                    .addPlaceholderReplace("%PROTECTED%", String.valueOf(counter.getProtectedBreakages()))
                    .addPlaceholderReplace("%BROKEN%", String.valueOf(counter.getBreakages()))
                    .returnMessage() + "\n";
        }

        if (toReturn.length() > 0) toReturn = toReturn.substring(0, toReturn.length() - 1);
        return toReturn;
    }

    private void notify(@Nullable CommandSender toNotify, BreakCounter counter, boolean checkIfAlreadyNotified) {

        if (counter.isBypassed()) return;

        if (toNotify instanceof Player) {
            if (checkIfAlreadyNotified && counter.isAlreadyInformed((Player) toNotify)) return;
            else counter.setInformed((Player) toNotify);
        }

        MessageBuilder message = MessageBuilder.create("modAntiXrayDetected", hmc, "AntiXRay")
                .addPlaceholderReplace("%PLAYER%", counter.getOwnerName())
                .addPlaceholderReplace("%BROKENTOTAL%", String.valueOf(counter.getBreakages()))
                .addPlaceholderReplace("%BROKENPROTECTED%", String.valueOf(counter.getProtectedBreakages()))
                .addPlaceholderReplace("%LASTLOCATION%", Utils.getStringFromLocation(counter.getLastProtectedLocation()))
                .addPlaceholderReplace("%WORLD%", counter.getLastProtectedLocation().getWorld().getName())
                .addPlaceholderReplace("%MATERIAL%", Utils.makeStringFriendly(counter.getLastMaterial().toString()));

        if (toNotify != null) message.sendMessage(toNotify);
        else message.broadcastMessage("hmc.antixray.notify", true);
    }

    private boolean counterExists(UUID player) {

        if (observedPlayers.containsKey(player)) {
            if (observedPlayers.get(player).hasExpired()) observedPlayers.remove(player);
        }

        return observedPlayers.containsKey(player);
    }

    @Override
    public void loadConfig() {

        timeUntilClear = hmc.getConfig().getInt("antiXray.intervalUntilClearSeconds", 300);
        protectedBlockThreshold = hmc.getConfig().getInt("antiXray.protectedBlockThreshold", 20);
        yLevelThreshold = hmc.getConfig().getInt("antiXray.yLevelThreshold", 30);
        protectedBlockRatio = hmc.getConfig().getDouble("antiXray.protectedBlockRatioThreshold", 0.04);

        protectedMaterial = Utils.stringListToMaterialSet(hmc.getConfig().getStringList("antiXray.protectedBlocks"));
    }

    @Override
    public void sweep() {
        observedPlayers.values().removeIf(BreakCounter::hasExpired);
    }

    private class BreakCounter {

        final UUID uuid;
        final Set<UUID> alreadyInformed = new HashSet<>();

        int blocksBroken = 1;
        int protectedBlocksBroken = 0;
        boolean bypass = false;
        Location pastProtectedLocation;
        Location lastProtectedLocation;

        Material lastProtectedBlock;
        long lastProtectedBreakTime = System.currentTimeMillis() / 1000;

        BreakCounter(final UUID uuid) {
            this.uuid = uuid;
        }

        String getOwnerName() {
            try {
                return storage.getPlayer(uuid).getName();
            } catch (PlayerNotFoundException e) {
                return "Error";
            }
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
            return pastProtectedLocation == null
                    || !pastProtectedLocation.getWorld().equals(lastProtectedLocation.getWorld())
                    || pastProtectedLocation.distance(lastProtectedLocation) > 3.0d;
        }

        int incrementBreakages() {
            return ++blocksBroken;
        }

        int incrementProtectedBlocksBroken(Block block) {
            lastProtectedBreakTime = System.currentTimeMillis() / 1000;
            pastProtectedLocation = lastProtectedLocation;
            lastProtectedLocation = block.getLocation();
            lastProtectedBlock = block.getType();
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
            return observedPermanently.contains(uuid);
        }

        Material getLastMaterial() {
            return lastProtectedBlock;
        }

        boolean hasExpired() {
            return !isBypassed()
                    && !isCheckedPermanently()
                    && lastProtectedBreakTime + timeUntilClear < System.currentTimeMillis() / 1000;
        }
    }
}
