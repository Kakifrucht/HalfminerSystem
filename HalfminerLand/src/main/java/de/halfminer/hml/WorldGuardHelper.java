package de.halfminer.hml;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.halfminer.hml.land.Land;
import org.bukkit.Chunk;
import org.bukkit.Location;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * This class handles WorldGuard integration. It creates regions for given {@link Land} and updates their states.
 * It queries current protection and fixes it on demand. Friends can be added through
 * {@link #addMemberToRegion(Land, UUID)} and removed via {@link #removeMemberFromRegion(Land, UUID)}.
 */
public class WorldGuardHelper {

    private static final String ALLOW_SHOP_FLAG_NAME = "allow-shop";
    private static final int HIGHEST_BLOCK_Y = 255;

    private final Logger logger;
    private final WorldGuard wg;


    WorldGuardHelper(Logger logger) {
        this.logger = logger;
        this.wg = WorldGuard.getInstance();
    }

    public boolean isLandFree(Land land) {
        RegionManager regionManager = getRegionManager(land.getWorld());
        return regionManager.getApplicableRegions(createRegionFromChunk(land.getChunk())).size() == 0;
    }

    public void updateRegionOfLand(Land land) {
        updateRegionOfLand(land, false, true);
    }

    public void updateRegionOfLand(Land land, boolean forceRefresh, boolean keepFriendsOnRefresh) {
        Chunk chunk = land.getChunk();

        RegionManager regionManager = getRegionManager(land.getWorld());
        DefaultDomain defaultDomain = null;
        ProtectedRegion region = getRegionFromRegionManager(chunk);

        if (land.hasOwner()) {

            // check if region has correct owner and is at correct location, else remove it (to cause a regeneration)
            if (region != null) {

                ProtectedRegion compareRegion = createRegionFromChunk(chunk);
                if (forceRefresh
                        || !region.getOwners().contains(land.getOwner().getUniqueId())
                        || !region.getMaximumPoint().equals(compareRegion.getMaximumPoint())
                        || !region.getMinimumPoint().equals(compareRegion.getMinimumPoint())) {

                    if (keepFriendsOnRefresh) {
                        defaultDomain = region.getMembers();
                    }

                    removeRegion(regionManager, region, land);
                    region = null;
                }
            }

            if (region == null) {

                // create region
                region = createRegionFromChunk(chunk);

                Flag<?> flag = wg.getFlagRegistry().get(ALLOW_SHOP_FLAG_NAME);
                if (flag instanceof StateFlag) {
                    region.setFlag((StateFlag) flag, StateFlag.State.ALLOW);
                }

                region.setFlag(Flags.USE, StateFlag.State.ALLOW);
                region.setFlag(Flags.PVP, StateFlag.State.DENY);
                region.setFlag(Flags.ENDERPEARL, StateFlag.State.DENY);

                if (defaultDomain != null) {
                    region.setMembers(defaultDomain);
                }

                defaultDomain = new DefaultDomain();
                defaultDomain.addPlayer(land.getOwner().getUniqueId());
                region.setOwners(defaultDomain);

                regionManager.addRegion(region);
                logger.info("Created region with id " + region.getId() + " for land " + land);
            }

            if (land.isAbandoned() != isAbandoned(region)) {
                if (land.isAbandoned()) {
                    region.setFlag(Flags.PVP, StateFlag.State.ALLOW);
                    region.setFlag(Flags.ENDERPEARL, StateFlag.State.ALLOW);

                    region.setFlag(Flags.BUILD, StateFlag.State.ALLOW);
                    region.setFlag(Flags.CHEST_ACCESS, StateFlag.State.ALLOW);
                    region.setFlag(Flags.TNT, StateFlag.State.ALLOW);

                    logger.info("Set region for land " + land + " abandoned");
                } else {
                    // force region refresh, instead of changing flags manually
                    updateRegionOfLand(land, true, keepFriendsOnRefresh);
                }
            }

        } else /* land has no owner, remove region */ {
            removeRegion(regionManager, region, land);
        }
    }

    private void removeRegion(RegionManager regionManager, ProtectedRegion region, Land land) {
        if (region != null) {
            regionManager.removeRegion(region.getId());
            logger.info("Removed region with id " + region.getId() + " for land " + land);
        }
    }

    public boolean isMarkedAbandoned(Land land) {
        ProtectedRegion region = getRegionFromRegionManager(land.getChunk());
        return region != null && isAbandoned(region);
    }

    private boolean isAbandoned(ProtectedRegion region) {
        return region.getFlag(Flags.BUILD) == StateFlag.State.ALLOW;
    }

    public DefaultDomain getMemberList(Land land) {

        ProtectedRegion region = getRegionFromRegionManager(land.getChunk());
        if (region == null) {
            throw new IllegalStateException("Region for land " + land.toString() + " not found");
        }

        return region.getMembers();
    }

    public boolean addMemberToRegion(Land land, UUID toAdd) {

        DefaultDomain memberList = getMemberList(land);
        if (memberList.contains(toAdd)) {
            return false;
        }

        memberList.addPlayer(toAdd);
        return true;
    }

    public boolean removeMemberFromRegion(Land land, UUID toRemove) {

        DefaultDomain memberList = getMemberList(land);
        if (!memberList.contains(toRemove)) {
            return false;
        }

        memberList.removePlayer(toRemove);
        return true;
    }

    boolean isPvPEnabled(Location loc) {

        Location location = loc.clone();
        if (loc.getBlockY() < 0) {
            location.setY(0d);
        } else if (loc.getBlockY() > HIGHEST_BLOCK_Y) {
            location.setY(HIGHEST_BLOCK_Y);
        }

        return getRegionManager(location.getWorld())
                .getApplicableRegions(BukkitAdapter.asBlockVector(loc))
                .queryValue(null, Flags.PVP) != StateFlag.State.DENY;
    }

    private ProtectedRegion getRegionFromRegionManager(Chunk chunk) {
        return getRegionManager(chunk.getWorld()).getRegion(getRegionName(chunk));
    }

    private ProtectedRegion createRegionFromChunk(Chunk chunk) {
        String regionName = getRegionName(chunk);
        BlockVector3 min = BukkitAdapter.asBlockVector(chunk.getBlock(0, 0, 0).getLocation());
        BlockVector3 max = BukkitAdapter.asBlockVector(chunk.getBlock(15, 255, 15).getLocation());
        return new ProtectedCuboidRegion(regionName, min, max);
    }

    private RegionManager getRegionManager(org.bukkit.World world) {
        World wgWorld = BukkitAdapter.adapt(world);
        return wg.getPlatform().getRegionContainer().get(wgWorld);
    }

    private String getRegionName(Chunk chunk) {
        return "hmland_" + chunk.getX() + "," + chunk.getZ();
    }
}
