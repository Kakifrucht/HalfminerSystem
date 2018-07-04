package de.halfminer.hml.land;

import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Pair;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementing class handles {@link Land} instances and returns land at a given location,
 * from teleport points, returns all lands owned by a player or the server, returns connected
 * land from a given land via {@link #getConnectedLand(Land)} and marks a land with particles.
 * It also returns a {@link FlyBoard} instance via {@link #getFlyBoard()}.
 */
public interface Board {

    /**
     * Method to be called when a player changes chunks.
     *
     * @param player player that moved
     * @param previousChunk chunk the player left
     * @param newChunk chunk the player entered
     * @return a pair, where the left entry is the previous land and the right entry is the new land
     */
    Pair<Land, Land> updatePlayerLocation(Player player, Chunk previousChunk, Chunk newChunk);

    /**
     * @param player player
     * @return land at players location
     */
    Land getLandAt(Player player);

    /**
     * @param location location
     * @return land at given location
     */
    Land getLandAt(Location location);

    /**
     * @param chunk chunk
     * @return land at given chunk
     */
    Land getLandAt(Chunk chunk);

    /**
     * @param teleportName the name of the teleport point
     * @return land with given teleport point or null if no land with given teleportName exists
     */
    Land getLandFromTeleport(String teleportName);

    /**
     * @param uuid uuid
     * @return a set containing all lands owned by given uuid
     */
    Set<Land> getLands(UUID uuid);

    /**
     * @param player player
     * @return a set containing all lands owned by given player
     */
    Set<Land> getLands(Player player);

    /**
     * @param player halfminer player that may be offline
     * @return a set containing all lands owned by given player
     */
    Set<Land> getLands(HalfminerPlayer player);

    /**
     * @param player player
     * @return a list containing all lands owned by a given player, that have a teleport point
     */
    List<Land> getLandsWithTeleport(Player player);

    /**
     * @return a set containing all lands owned by the server
     */
    Set<Land> getLandsOfServer();

    /**
     * @return a set containing all lands owned by a player or the server
     */
    Set<Land> getOwnedLandSet();

    /**
     * Get all connected lands that are owned by the same player. If the land is not owned this method
     * will return a set consiting only of this land. The given land parameter is always part of the returned set.
     *
     * To prevent locking the main thread for too long the amount of iterations may be limited, and thus not
     * every connected land may be returned.
     *
     * @param land to get all connected lands of that are owned by the same player
     * @return a set containing all lands that are connected to the given land and owned by the same player
     */
    Set<Land> getConnectedLand(Land land);

    /**
     * Display particles that will mark this chunk.
     *
     * @param player to show the particles to
     * @param showParticles land where the particles will be displayed
     */
    void showChunkParticles(Player player, Land showParticles);

    /**
     * If the lands state was updated (for example, when a teleport point was added).
     *
     * @param land land that was updated
     */
    void landWasUpdated(Land land);

    /**
     * @return a FlyBoard instance associated with this Board
     */
    FlyBoard getFlyBoard();
}
