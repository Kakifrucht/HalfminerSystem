package de.halfminer.hml.land;

import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Pair;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.*;

class LandMap {

    private final Map<Pair<World, Pair<Integer, Integer>>, Land> chunkLandMap;
    private final Map<String, Land> teleportMap;

    private final Map<HalfminerPlayer, List<Land>> ownedLandMap;


    LandMap() {
        this.chunkLandMap = new HashMap<>();
        this.teleportMap = new HashMap<>();

        this.ownedLandMap = new HashMap<>();
    }

    void addLand(Land land) {

        Pair<World, Pair<Integer, Integer>> chunkPair = getPairFromChunk(land.getChunk());
        if (chunkLandMap.containsKey(chunkPair)) {
            removeLand(land);
        }

        chunkLandMap.put(chunkPair, land);

        if (land.hasOwner() && !land.isServerLand()) {
            if (ownedLandMap.containsKey(land.getOwner())) {
                ownedLandMap.get(land.getOwner()).add(land);
            } else {
                List<Land> newList = new ArrayList<>();
                newList.add(land);
                ownedLandMap.put(land.getOwner(), newList);
            }
        }

        if (land.hasTeleportLocation()) {
            teleportMap.put(land.getTeleportName(), land);
        }
    }

    void removeLand(Land land) {

        chunkLandMap.remove(getPairFromChunk(land.getChunk()));
        if (land.hasTeleportLocation()) {
            teleportMap.remove(land.getTeleportName());
        }

        if (land.hasOwner() && ownedLandMap.containsKey(land.getOwner())) {
            List<Land> ownedLand = ownedLandMap.get(land.getOwner());
            ownedLand.remove(land);
            if (ownedLand.isEmpty()) {
                ownedLandMap.remove(land.getOwner());
            }
        }
    }

    Land getLandAt(Chunk chunk) {

        Pair<World, Pair<Integer, Integer>> pair = getPairFromChunk(chunk);
        if (chunkLandMap.containsKey(pair)) {
            return chunkLandMap.get(pair);
        }

        return null;
    }

    Land getLandFromTeleport(String teleport) {

        Land land = teleportMap.get(teleport);
        if (land != null) {
            if (land.hasTeleportLocation() && land.getTeleportName().equals(teleport)) {
                return land;
            } else {
                teleportMap.remove(teleport);
                return null;
            }
        }

        return null;
    }

    List<Land> getOwnedLandList(HalfminerPlayer halfminerPlayer) {

        List<Land> ownedList = ownedLandMap.getOrDefault(halfminerPlayer, Collections.emptyList());
        ownedList.removeIf(land -> !land.hasOwner() || !land.getOwner().equals(halfminerPlayer) || land.isServerLand());
        if (ownedList.isEmpty()) {
            ownedLandMap.remove(halfminerPlayer);
        }

        return new ArrayList<>(ownedList);
    }

    Collection<Land> getLandCollection() {
        return new HashSet<>(chunkLandMap.values());
    }

    void cleanUp() {
        chunkLandMap.values().removeIf(Land::canBeRemoved);
    }

    private Pair<World, Pair<Integer, Integer>> getPairFromChunk(Chunk chunk) {
        Pair<Integer, Integer> coordinatePair = new Pair<>(chunk.getX(), chunk.getZ());
        return new Pair<>(chunk.getWorld(), coordinatePair);
    }
}
