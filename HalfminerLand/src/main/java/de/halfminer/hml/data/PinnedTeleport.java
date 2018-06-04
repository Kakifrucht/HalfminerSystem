package de.halfminer.hml.data;

import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.Material;

/**
 * Wrapper class for stored pinned teleports.
 */
public class PinnedTeleport {

    private final String teleport;
    private final HalfminerPlayer owner;
    private final String ownerName;
    private final Material material;


    public PinnedTeleport(Land teleportLand, Material material) {
        this.teleport = teleportLand.getTeleportName();
        this.owner = teleportLand.isServerLand() ? null : teleportLand.getOwner();
        this.ownerName = teleportLand.getOwnerName();
        this.material = material;
    }

    public String getTeleport() {
        return teleport;
    }

    public HalfminerPlayer getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean hasMaterial() {
        return material != null;
    }

    public Material getMaterial() {
        return material;
    }

    @Override
    public String toString() {
        return teleport + "," + (material != null ? material.name() : "null");
    }
}
