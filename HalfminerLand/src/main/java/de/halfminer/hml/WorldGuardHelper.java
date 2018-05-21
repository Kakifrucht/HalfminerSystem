package de.halfminer.hml;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.BukkitUtil;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.halfminer.hml.land.Land;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.plugin.PluginManager;

import java.util.UUID;
import java.util.logging.Logger;

public class WorldGuardHelper {

    private static final int HIGHEST_BLOCK_Y = 255;

    private final Logger logger;
    private final WorldGuardPlugin wg;


    WorldGuardHelper(Logger logger, PluginManager pluginManager) {
        this.logger = logger;
        this.wg = (WorldGuardPlugin) pluginManager.getPlugin("WorldGuard");
    }

    public boolean isLandFree(Land land) {
        RegionManager regionManager = wg.getRegionManager(land.getWorld());
        return regionManager.getApplicableRegions(createRegionFromChunk(land.getChunk())).size() == 0;
    }

    public void updateRegionOfLand(Land land) {
        updateRegionOfLand(land, false, true);
    }

    @SuppressWarnings("deprecation")
    public void updateRegionOfLand(Land land, boolean forceRefresh, boolean keepFriendsOnRefresh) {
        Chunk chunk = land.getChunk();

        RegionManager regionManager = wg.getRegionManager(chunk.getWorld());
        DefaultDomain defaultDomain = null;
        ProtectedRegion region = getRegionFromRegionManager(chunk);

        if (land.hasOwner()) {

            // check if region has correct owner and is at correct location, else recreate
            if (region != null) {

                ProtectedRegion compareRegion = createRegionFromChunk(chunk);
                if (forceRefresh
                        || !region.getOwners().contains(land.getOwner().getUniqueId())
                        || !region.getMaximumPoint().equals(compareRegion.getMaximumPoint())
                        || !region.getMinimumPoint().equals(compareRegion.getMinimumPoint())) {

                    if (keepFriendsOnRefresh) {
                        defaultDomain = region.getMembers();
                    }

                    regionManager.removeRegion(region.getId());
                    region = null;
                }
            }

            if (region == null) {

                // create region
                region = createRegionFromChunk(chunk);

                region.setFlag(DefaultFlag.ENABLE_SHOP, StateFlag.State.ALLOW);
                region.setFlag(DefaultFlag.USE, StateFlag.State.ALLOW);
                region.setFlag(DefaultFlag.PVP, StateFlag.State.DENY);
                region.setFlag(DefaultFlag.ENDERPEARL, StateFlag.State.DENY);

                if (defaultDomain != null) {
                    region.setMembers(defaultDomain);
                }

                defaultDomain = new DefaultDomain();
                defaultDomain.addPlayer(land.getOwner().getUniqueId());
                region.setOwners(defaultDomain);

                regionManager.addRegion(region);
                logger.info("Created region with id " + region.getId() + " for land " + land);
            }

            if (land.isAbandoned() != isMarkedAbandoned(land)) {
                if (land.isAbandoned()) {
                    region.setFlag(DefaultFlag.PVP, StateFlag.State.ALLOW);
                    region.setFlag(DefaultFlag.ENDERPEARL, StateFlag.State.ALLOW);

                    region.setFlag(DefaultFlag.BUILD, StateFlag.State.ALLOW);
                    region.setFlag(DefaultFlag.CHEST_ACCESS, StateFlag.State.ALLOW);
                    region.setFlag(DefaultFlag.TNT, StateFlag.State.ALLOW);

                    logger.info("Set region for land " + land + " abandoned");
                } else {
                    // force refresh region fully
                    updateRegionOfLand(land, true, keepFriendsOnRefresh);
                }
            }

        } else {

            // delete region if exists
            if (region != null) {
                regionManager.removeRegion(region.getId());
                logger.info("Removed region with id " + region.getId() + " for land " + land);
            }
        }
    }

    public boolean isMarkedAbandoned(Land land) {
        ProtectedRegion region = getRegionFromRegionManager(land.getChunk());
        return region != null && region.getFlag(DefaultFlag.BUILD) == StateFlag.State.ALLOW;
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
            location.setY((double) HIGHEST_BLOCK_Y);
        }

        return wg.getRegionManager(location.getWorld())
                .getApplicableRegions(location)
                .queryValue(null, DefaultFlag.PVP) != StateFlag.State.DENY;
    }

    private ProtectedRegion getRegionFromRegionManager(Chunk chunk) {
        return wg.getRegionManager(chunk.getWorld()).getRegion(getRegionName(chunk));
    }

    private ProtectedRegion createRegionFromChunk(Chunk chunk) {
        String regionName = getRegionName(chunk);
        BlockVector min = BukkitUtil.toVector(chunk.getBlock(0, 0, 0));
        BlockVector max = BukkitUtil.toVector(chunk.getBlock(15, 255, 15));
        return new ProtectedCuboidRegion(regionName, min, max);
    }

    private String getRegionName(Chunk chunk) {
        return "hmland_" + chunk.getX() + "," + chunk.getZ();
    }
}
