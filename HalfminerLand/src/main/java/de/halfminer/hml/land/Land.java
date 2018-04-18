package de.halfminer.hml.land;

import de.halfminer.hml.LandClass;
import de.halfminer.hml.WorldGuardHelper;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class Land extends LandClass {

    private static final char PATH_COORDINATE_SPLIT_CHAR = 'z';
    private static final String STORAGE_OWNER = ".owner";
    private static final String STORAGE_IS_FREE = ".isFreeLand";
    private static final String STORAGE_TELEPORT = ".teleport";
    private static final String STORAGE_TELEPORT_NAME = ".teleport.name";
    private static final String STORAGE_TELEPORT_LOCATION = ".teleport.location";

    private final WorldGuardHelper wgh;
    private final Chunk chunk;

    private final ConfigurationSection mapSection;
    private final String path;

    private HalfminerPlayer owner;
    private boolean isFreeLand = false;

    private String teleportName;
    private Location teleportLocation;

    private int playersOnLand;
    private boolean isAbandoned = false;


    Land(Chunk chunk, ConfigurationSection mapSection) {

        this.wgh = hml.getWorldGuardHelper();
        this.chunk = chunk;

        this.mapSection = mapSection;
        this.path = chunk.getWorld().getName() + '.' + chunk.getX() + PATH_COORDINATE_SPLIT_CHAR + chunk.getZ();

        // load if data is stored
        if (mapSection.isConfigurationSection(path)) {

            ConfigurationSection landSection = mapSection.getConfigurationSection(path);
            if (landSection.contains(STORAGE_OWNER.substring(1))) {
                String uuidString = landSection.getString(STORAGE_OWNER.substring(1));
                try {
                    owner = hms.getStorageHandler().getPlayer(UUID.fromString(uuidString));
                } catch (PlayerNotFoundException e) {
                    hml.getLogger().warning("Player with UUID " + uuidString
                            + "' not found, skipping chunk at " + chunk.getX() + "-" + chunk.getZ());
                }

                wgh.updateRegionOfLand(this);
            }

            isFreeLand = landSection.getBoolean(STORAGE_IS_FREE.substring(1), false);

            if (landSection.contains(STORAGE_TELEPORT_NAME)) {
                teleportName = landSection.getString(STORAGE_TELEPORT_NAME.substring(1));
                teleportLocation = (Location) landSection.get(STORAGE_TELEPORT_LOCATION.substring(1));
            }
        }
    }

    public BuyableStatus getBuyableStatus() {

        if (hasOwner()) {
            return BuyableStatus.ALREADY_OWNED;
        }

        if (!wgh.isLandFree(this)) {
            return BuyableStatus.LAND_NOT_BUYABLE;
        }

        if (playersOnLand > 1) {
            return BuyableStatus.OTHER_PLAYERS_ON_LAND;
        }

        return BuyableStatus.BUYABLE;
    }

    public SellableStatus getSellableStatus(UUID uuid) {

        if (!hasOwner()) {
            return SellableStatus.NO_OWNER;
        }

        if (!owner.getUniqueId().equals(uuid)) {
            return SellableStatus.NOT_OWNED;
        }

        if (playersOnLand > 1) {
            return SellableStatus.OTHER_PLAYERS_ON_LAND;
        }

        if (hasTeleportLocation()) {
            return SellableStatus.HAS_TELEPORT;
        }

        return SellableStatus.SELLABLE;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public boolean hasOwner() {
        return owner != null;
    }

    public HalfminerPlayer getOwner() {
        return owner;
    }

    public void setOwner(HalfminerPlayer owner) {
        this.owner = owner;
        wgh.updateRegionOfLand(this);

        if (owner != null) {
            mapSection.set(path + STORAGE_OWNER, owner.getUniqueId().toString());
        } else {
            setTeleport(null, null);
            setFreeLand(false);
            mapSection.set(path, null);
        }
    }

    public void setFreeLand(boolean isFreeLand) {
        this.isFreeLand = isFreeLand;
        mapSection.set(path + STORAGE_IS_FREE, isFreeLand ? true : null);
    }

    public boolean isFreeLand() {
        return isFreeLand;
    }

    public boolean isAbandoned() {
        //TODO logic for abandoned land, depending on player last seen time
        return isAbandoned;
    }

    public boolean hasTeleportLocation() {
        return teleportLocation != null;
    }

    public String getTeleportName() {
        return teleportName;
    }

    public Location getTeleportLocation() {
        return teleportLocation;
    }

    public void setTeleport(String teleportName, Location teleportLocation) {
        this.teleportName = teleportName;
        this.teleportLocation = teleportLocation;

        if (hasTeleportLocation()) {
            mapSection.set(path + STORAGE_TELEPORT_NAME, teleportName);
            mapSection.set(path + STORAGE_TELEPORT_LOCATION, teleportLocation);
        } else {
            mapSection.set(path + STORAGE_TELEPORT, null);
        }
    }

    void playerEntered() {
        playersOnLand++;
    }

    /**
     * Method to be called if a player leaves this land.
     *
     * @return true if this object can be discarded
     * @throws IllegalStateException when player counter is already zero
     */
    boolean playerLeft() {
        if (playersOnLand == 0) {
            throw new IllegalStateException("Method called for already empty land (" + toString() + ")");
        }

        playersOnLand--;
        return owner == null && playersOnLand == 0;
    }

    /**
     * Check if a player can interact on this land.
     *
     * @param player to check
     * @return true if land is free, player is land owner or added as member of land
     */
    public boolean hasPermission(Player player) {

        if (hasOwner()) {
            return owner.getUniqueId().equals(player.getUniqueId())
                    || wgh.getMemberList(this).getUniqueIds().contains(player.getUniqueId());
        }

        return true;
    }

    public Set<UUID> getMemberSet() {
        return wgh.getMemberList(this).getUniqueIds();
    }

    public boolean addMember(UUID toAdd) {
        return wgh.addMemberToRegion(this, toAdd);
    }

    public boolean removeMember(UUID toRemove) {
        return wgh.removeMemberFromRegion(this, toRemove);
    }

    @Override
    public String toString() {
        return chunk.getWorld().getName() + ",x" + chunk.getX() + ",z" + chunk.getZ();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Land land = (Land) o;
        return toString().equals(land.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }


    public enum BuyableStatus {
        OTHER_PLAYERS_ON_LAND,
        ALREADY_OWNED,
        LAND_NOT_BUYABLE,
        BUYABLE
    }

    public enum SellableStatus {
        NO_OWNER,
        NOT_OWNED,
        OTHER_PLAYERS_ON_LAND,
        HAS_TELEPORT,
        SELLABLE
    }
}
