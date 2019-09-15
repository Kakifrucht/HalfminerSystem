package de.halfminer.hmc.module;

import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
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
import java.util.Objects;
import java.util.logging.Level;

/**
 * - Limits redstone usage
 *   - Redstone will not work if triggered too often
 * - Limits piston usage
 *   - Only a given amount of pistons can be extended in a given time in the pistons region
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
    private final Map<PistonRegion, Integer> pistonCount = new HashMap<>();

    // Redstone and pistons config
    private int howManyRedstoneUpdatesAllowed;
    private int howManyPistonsAllowed;
    private int pistonLimitRegionSize;

    // Hopper limit config
    private int hopperLimit;
    private int hopperLimitRadius;
    private boolean logHopperLimit;

    // Entity limits
    private int entityLimitLiving;
    private int entityLimitSame;
    private int boxSize;


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void countPistonExtend(BlockPistonExtendEvent e) {

        World world = e.getBlock().getWorld();
        int xIndex = e.getBlock().getX() / pistonLimitRegionSize;
        int zIndex = e.getBlock().getZ() / pistonLimitRegionSize;
        PistonRegion region = new PistonRegion(world, xIndex, zIndex);
        if (pistonCount.containsKey(region)) {
            int countInRegion = pistonCount.get(region);
            if (++countInRegion > howManyPistonsAllowed) {
                e.setCancelled(true);
            } else {
                pistonCount.put(region, countInRegion);
            }
        } else {
            pistonCount.put(region, 1);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void countRedstoneChange(BlockRedstoneEvent e) {
        Location redstoneLoc = e.getBlock().getLocation();
        if (firedAt.containsKey(redstoneLoc)) {
            int amount = firedAt.get(redstoneLoc);
            if (amount > howManyRedstoneUpdatesAllowed) {
                e.setNewCurrent(0);
            } else {
                firedAt.put(redstoneLoc, amount + 1);
            }
        } else {
            firedAt.put(redstoneLoc, 1);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void checkHopperLimit(BlockPlaceEvent e) {

        if (e.getPlayer().hasPermission("hmc.bypass.hopperlimit")) return;

        Block block = e.getBlock();
        if (block.getType() == Material.HOPPER && tooManyHoppers(block.getLocation())) {
            e.setCancelled(true);
            MessageBuilder.create("modPerformanceReachedHopper", hmc, "Info").sendMessage(e.getPlayer());
            if (logHopperLimit) {
                MessageBuilder.create("modPerformanceReachedHopperLog", hmc)
                        .addPlaceholder("%PLAYER%", e.getPlayer().getName())
                        .addPlaceholder("%LIMIT%", hopperLimit)
                        .addPlaceholder("%LOCATION%", Utils.getStringFromLocation(block.getLocation()))
                        .logMessage(Level.INFO);
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

        ConfigurationSection config = hmc.getConfig().getConfigurationSection("performance");

        int ticksDelayUntilClear = config.getInt("ticksDelayUntilClear", 160);
        howManyRedstoneUpdatesAllowed = config.getInt("howManyRedstoneUpdatesAllowed", 32);
        howManyPistonsAllowed = config.getInt("howManyPistonsAllowed", 200);
        pistonLimitRegionSize = config.getInt("pistonLimitRegionSize", 64);
        hopperLimit = config.getInt("hopperLimit", 64);
        hopperLimitRadius = config.getInt("hopperLimitRadius", 7);
        logHopperLimit = config.getBoolean("hopperLimitLog", false);
        entityLimitLiving = config.getInt("entityLimitLiving", 100);
        entityLimitSame = config.getInt("entityLimitSame", 25);
        boxSize = config.getInt("boxSize", 16);

        if (clearTask != null) {
            clearTask.cancel();
        }

        clearTask = scheduler.runTaskTimer(hmc, () -> {
            firedAt.clear();
            pistonCount.clear();
        }, ticksDelayUntilClear, ticksDelayUntilClear);
    }

    private static class PistonRegion {

        private final World world;
        private final int x;
        private final int z;


        PistonRegion(World world, int x, int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PistonRegion otherRegion = (PistonRegion) o;
            return x == otherRegion.x && z == otherRegion.z && world.equals(otherRegion.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, z);
        }
    }
}
