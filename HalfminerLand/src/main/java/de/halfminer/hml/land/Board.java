package de.halfminer.hml.land;

import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Pair;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface Board {

    Pair<Land, Land> updatePlayerLocation(Player player, Chunk previousChunk, Chunk newChunk);

    Land getLandAt(Player player);

    Land getLandAt(Location location);

    Land getLandAt(Chunk chunk);

    Land getLandFromTeleport(String teleportName);

    Set<Land> getLands(UUID uuid);

    Set<Land> getLands(Player player);

    Set<Land> getLands(HalfminerPlayer player);

    List<Land> getLandsWithTeleport(Player player);

    Set<Land> getLandsOfServer();

    Set<Land> getOwnedLandSet();

    Set<Land> getConnectedLand(Land land);

    void showChunkParticles(Player player, Land showParticles);

    void landWasUpdated(Land land);

    FlyBoard getFlyBoard();
}
