package de.halfminer.hml.land.contract;

import de.halfminer.hml.LandClass;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.land.Land;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

public abstract class AbstractContract extends LandClass {

    final LandStorage landStorage;

    final Player player;
    private final Chunk chunk;

    private boolean canBeFulfilled;
    private boolean wasFulfilled;


    AbstractContract(Player player, Land land) {
        super(false);

        this.landStorage = hml.getLandStorage();

        this.player = player;
        this.chunk = land.getChunk();

        this.canBeFulfilled = false;
        this.wasFulfilled = false;
    }

    public Player getPlayer() {
        return player;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public boolean canBeFulfilled() {
        return canBeFulfilled && !wasFulfilled;
    }

    public void setCanBeFulfilled() {
        canBeFulfilled = true;
    }

    public boolean wasFulfilled() {
        return wasFulfilled;
    }

    public void fulfillContract(Land land) {
        if (canBeFulfilled()) {
            fulfill(land);
            this.wasFulfilled = true;
        } else {
            hml.getLogger().warning("Illegal repeated call of fulfillContract() for land " + land);
        }
    }

    abstract void fulfill(Land land);

    public abstract double getCost();
}
