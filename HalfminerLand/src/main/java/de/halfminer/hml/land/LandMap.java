package de.halfminer.hml.land;

import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Pair;
import org.bukkit.Chunk;
import org.bukkit.World;

import java.util.*;

class LandMap {

    private final Map<Pair<World, Pair<Integer, Integer>>, Land> chunkLandMap;
    private final Map<String, Land> teleportMap;

    private final Map<HalfminerPlayer, Set<Land>> ownedLandMap;


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

        if (land.hasOwner()) {
            if (ownedLandMap.containsKey(land.getOwner())) {
                ownedLandMap.get(land.getOwner()).add(land);
            } else {
                HashSet<Land> newSet = new HashSet<>();
                newSet.add(land);
                ownedLandMap.put(land.getOwner(), newSet);
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
            Set<Land> ownedLand = ownedLandMap.get(land.getOwner());
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
        if (land == null) {
            return null;
        } else if (land.hasTeleportLocation() && land.getTeleportName().equals(teleport)) {
            return land;
        } else {
            teleportMap.remove(teleport);
            return null;
        }
    }

    Set<Land> getOwnedLandSet(HalfminerPlayer halfminerPlayer) {
        Set<Land> set = ownedLandMap.getOrDefault(halfminerPlayer, Collections.emptySet());
        set.removeIf(l -> !l.hasOwner() || !l.getOwner().equals(halfminerPlayer));
        if (set.isEmpty()) {
            ownedLandMap.remove(halfminerPlayer);
        }
        return set;
    }

    Collection<Land> getLandCollection() {
        return chunkLandMap.values();
    }

    private Pair<World, Pair<Integer, Integer>> getPairFromChunk(Chunk chunk) {
        Pair<Integer, Integer> coordinatePair = new Pair<>(chunk.getX(), chunk.getZ());
        return new Pair<>(chunk.getWorld(), coordinatePair);
    }
}
