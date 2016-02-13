package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Reduce server strain by limiting redstone, pistons, hoppers and spawners
 * - Redstone can only be triggered a given amount locally
 * - Pistons can only be triggered a given amount server wide
 * - Hoppers placement is limited
 * - Amount of mobspawns are limited
 */
public class ModPerformance extends HalfminerModule implements Listener {

    // Storage for limitations
    private final Map<Location, Integer> lastStored = new HashMap<>();
    private int pistonCount = 0;
    private int taskId = 0;
    // Redstone and pistons config
    private int howMuchRedstoneAllowed;
    private int howManyPistonsAllowed;
    // Hopper limit config
    private int hopperLimit;
    private int hopperLimitRadius;
    private boolean logHopperLimit;
    private String hopperLimitMessage;
    // Entity limits
    private int entityLimitLiving;
    private int entityLimitSame;
    private int boxSize;

    public ModPerformance() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void countPistonExtend(BlockPistonExtendEvent e) {
        e.setCancelled(increasePistonCount());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void countPistonRetract(BlockPistonRetractEvent e) {
        e.setCancelled(increasePistonCount());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    @SuppressWarnings("unused")
    public void countRedstoneChange(BlockRedstoneEvent e) {
        Location redstoneLoc = e.getBlock().getLocation();
        if (lastStored.containsKey(redstoneLoc)) {
            int amount = lastStored.get(redstoneLoc);
            if (amount > howMuchRedstoneAllowed) e.setNewCurrent(0);
            else lastStored.put(redstoneLoc, amount + 1);
        } else lastStored.put(redstoneLoc, 1);
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void checkHopperLimit(BlockPlaceEvent e) {
        if (e.getPlayer().hasPermission("hms.bypass.hopperlimit")) return;
        Block block = e.getBlock();
        if (block.getType() == Material.HOPPER) {
            if (tooManyHoppers(block.getLocation())) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(hopperLimitMessage);
                if (logHopperLimit) {
                    hms.getLogger().info(Language.getMessagePlaceholders("modPerformanceReachedHopperLog", false,
                            "%PLAYER%", e.getPlayer().getName(), "%LIMIT%", String.valueOf(hopperLimit), "%LOCATION%",
                            Language.getStringFromLocation(block.getLocation())));
                }
            }
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onMobSpawnerLimit(CreatureSpawnEvent e) {

        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();

        if (reason.equals(CreatureSpawnEvent.SpawnReason.NATURAL)
                || reason.equals(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT)
                || reason.equals(CreatureSpawnEvent.SpawnReason.MOUNT)
                || reason.equals(CreatureSpawnEvent.SpawnReason.JOCKEY)) return;

        Location loc = e.getEntity().getLocation();
        int amountLiving = 0;
        int amountSame = 0;
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, boxSize, boxSize, boxSize);
        if (nearby.size() < entityLimitLiving) return;
        for (Entity entity : nearby) {

            if (entity.getType().equals(e.getEntityType())) amountSame++;
            else if (entity instanceof LivingEntity && entity.isValid()) amountLiving++;

            if (amountLiving > entityLimitLiving || amountSame > entityLimitSame) {
                e.setCancelled(true);
                return;
            }
        }
    }

    private boolean increasePistonCount() {
        if (pistonCount > howManyPistonsAllowed) return true;
        else {
            pistonCount++;
            return false;
        }
    }

    private boolean tooManyHoppers(Location loc) {
        int hopperCount = 0;
        for (int x = loc.getBlockX() - hopperLimitRadius; x <= loc.getBlockX() + hopperLimitRadius; x++) {
            for (int y = loc.getBlockY() - hopperLimitRadius; y <= loc.getBlockY() + hopperLimitRadius; y++) {
                for (int z = loc.getBlockZ() - hopperLimitRadius; z <= loc.getBlockZ() + hopperLimitRadius; z++) {
                    if (loc.getWorld().getBlockAt(x, y, z).getType() == Material.HOPPER) hopperCount++;
                }
            }
        }
        return hopperCount > hopperLimit;
    }


    @Override
    public void reloadConfig() {

        int ticksDelayUntilClear = hms.getConfig().getInt("performance.ticksDelayUntilClear", 160);
        howMuchRedstoneAllowed = hms.getConfig().getInt("performance.howMuchRedstoneAllowed", 32);
        howManyPistonsAllowed = hms.getConfig().getInt("performance.howManyPistonsAllowed", 400);
        hopperLimit = hms.getConfig().getInt("performance.hopperLimit", 64);
        hopperLimitRadius = hms.getConfig().getInt("performance.hopperLimitRadius", 7);
        logHopperLimit = hms.getConfig().getBoolean("performance.hopperLimitLog", false);
        entityLimitLiving = hms.getConfig().getInt("performance.entitiyLimitLiving", 100);
        entityLimitSame = hms.getConfig().getInt("performance.entityLimitSame", 25);
        boxSize = hms.getConfig().getInt("performance.boxSize", 16);

        hopperLimitMessage = Language.getMessagePlaceholders("modPerformanceReachedHopper", true, "%PREFIX%", "Info");

        if (taskId != 0) hms.getServer().getScheduler().cancelTask(taskId);
        taskId = hms.getServer().getScheduler().scheduleSyncRepeatingTask(hms, new Runnable() {
            @Override
            public void run() {
                lastStored.clear();
                pistonCount = 0;
            }
        }, ticksDelayUntilClear, ticksDelayUntilClear);
    }
}
