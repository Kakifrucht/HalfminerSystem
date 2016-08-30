package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * - Limits redstone usage
 *   - Redstone will not work if triggered too often
 * - Limits piston usage
 *   - Only a given amount of pistons can be extended in a given time
 * - Limits hopper placement
 *   - Checks radius, if too many hoppers denies placement
 * - Limits mobspawns
 *   - Checks radius, if too many mobs stops the mobspawn
 */
@SuppressWarnings("unused")
public class ModPerformance extends HalfminerModule implements Listener {

    // Storage for limitations
    private BukkitTask clearTask;
    private final Map<Location, Integer> firedAt = new HashMap<>();
    private int pistonCount = 0;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void countPistonExtend(BlockPistonExtendEvent e) {

        if (pistonCount > howManyPistonsAllowed) e.setCancelled(true);
        else pistonCount++;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void countRedstoneChange(BlockRedstoneEvent e) {
        Location redstoneLoc = e.getBlock().getLocation();
        if (firedAt.containsKey(redstoneLoc)) {
            int amount = firedAt.get(redstoneLoc);
            if (amount > howMuchRedstoneAllowed) e.setNewCurrent(0);
            else firedAt.put(redstoneLoc, amount + 1);
        } else firedAt.put(redstoneLoc, 1);
    }

    @EventHandler(ignoreCancelled = true)
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
    public void onMobSpawnLimit(CreatureSpawnEvent e) {

        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();

        if (reason.equals(CreatureSpawnEvent.SpawnReason.NATURAL)
                || reason.equals(CreatureSpawnEvent.SpawnReason.SLIME_SPLIT)
                || reason.equals(CreatureSpawnEvent.SpawnReason.MOUNT)
                || reason.equals(CreatureSpawnEvent.SpawnReason.JOCKEY)
                || reason.equals(CreatureSpawnEvent.SpawnReason.DEFAULT)) return;

        Location loc = e.getEntity().getLocation();
        Collection<Entity> nearby = loc.getWorld().getNearbyEntities(loc, boxSize, boxSize, boxSize);
        if (nearby.size() < entityLimitLiving) return;

        int amountLiving = 0;
        int amountSame = 0;
        for (Entity entity : nearby) {

            if (!entity.isValid() || entity instanceof ArmorStand) continue;

            if (entity.getType().equals(e.getEntityType())) {
                amountSame++;
                amountLiving++;
            } else if (entity instanceof LivingEntity) amountLiving++;

            if (amountLiving > entityLimitLiving || amountSame > entityLimitSame) {
                e.setCancelled(true);
                return;
            }
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
    public void loadConfig() {

        int ticksDelayUntilClear = hms.getConfig().getInt("performance.ticksDelayUntilClear", 160);
        howMuchRedstoneAllowed = hms.getConfig().getInt("performance.howMuchRedstoneAllowed", 32);
        howManyPistonsAllowed = hms.getConfig().getInt("performance.howManyPistonsAllowed", 200);
        hopperLimit = hms.getConfig().getInt("performance.hopperLimit", 64);
        hopperLimitRadius = hms.getConfig().getInt("performance.hopperLimitRadius", 7);
        logHopperLimit = hms.getConfig().getBoolean("performance.hopperLimitLog", false);
        entityLimitLiving = hms.getConfig().getInt("performance.entitiyLimitLiving", 100);
        entityLimitSame = hms.getConfig().getInt("performance.entityLimitSame", 25);
        boxSize = hms.getConfig().getInt("performance.boxSize", 16);

        hopperLimitMessage = Language.getMessagePlaceholders("modPerformanceReachedHopper", true, "%PREFIX%", "Info");

        if (clearTask != null) clearTask.cancel();
        clearTask = scheduler.runTaskTimer(hms, () -> {
            firedAt.clear();
            pistonCount = 0;
        }, ticksDelayUntilClear, ticksDelayUntilClear);
    }
}
