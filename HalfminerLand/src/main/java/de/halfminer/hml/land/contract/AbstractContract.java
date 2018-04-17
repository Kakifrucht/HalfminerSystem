package de.halfminer.hml.land.contract;

import de.halfminer.hml.LandClass;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.handler.HanStorage;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class AbstractContract extends LandClass {

    private final HanStorage landStorage;
    final Player player;
    private final Chunk chunk;

    boolean canBeFulfilled;
    private final long creationTimestamp;


    AbstractContract(Player player, Land land) {
        super(false);
        this.landStorage = hml.getLandStorage();
        this.player = player;
        this.chunk = land.getChunk();

        this.canBeFulfilled = false;
        this.creationTimestamp = System.currentTimeMillis();
    }

    public Player getPlayer() {
        return player;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public boolean canBeFulfilled() {
        return canBeFulfilled;
    }

    public void setCanBeFulfilled() {
        canBeFulfilled = true;
    }

    double getLastCostFromStorage() {
        String path = getPath(player.getUniqueId());

        List<String> previousCostList = getStringListFromPath(path);
        if (previousCostList.isEmpty()) {
            return 0d;
        }

        return Double.valueOf(previousCostList.get(previousCostList.size() - 1));
    }

    void addCurrentCostToStorage(double cost) {

        String path = getPath(player.getUniqueId());

        List<String> previousCostList = getStringListFromPath(path);
        previousCostList.add(String.valueOf(cost));

        landStorage.set(path, previousCostList);
    }

    void removeLastCostFromStorage(UUID uuid) {

        String path = getPath(uuid);

        List<String> previousCostList = getStringListFromPath(path);
        if (!previousCostList.isEmpty()) {
            previousCostList.remove(previousCostList.size() - 1);
        }

        if (previousCostList.isEmpty()) {
            landStorage.set(path, null);
        } else {
            landStorage.set(path, previousCostList);
        }
    }

    private String getPath(UUID uuid) {
        return uuid.toString() + ".previousCostList";
    }

    private List<String> getStringListFromPath(String path) {

        List<String> previousCostList = landStorage.getRootSection().getStringList(path);

        if (previousCostList == null) {
            previousCostList = new ArrayList<>();
        }

        return previousCostList;
    }

    public abstract void fulfill(Land land);

    public abstract double getCost();
}
