package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.enums.GameModeType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

public interface Arena {

    /**
     * @return true if arena is configured and ready to go, false if it's not ready yet
     */
    boolean isActive();

    /**
     * @return true if arena is currently free and thus can be entered,
     *          false if in use or if {@link #isActive()} is false
     */
    boolean isFree();

    /**
     * @return the arenas name
     */
    String getName();

    /**
     * @return the arenas gamemode
     */
    GameModeType getGameMode();

    /**
     * Set the spawn points of the arena
     *
     * @param spawns list containing all spawnpoints
     */
    void setSpawns(List<Location> spawns);

    /**
     * Set the specified spawn to the desired location
     *
     * @param spawn location to set the spawn to
     * @param spawnNumber integer, if lower than 0 or above total number an additional spawn point will be added
     */
    void setSpawn(Location spawn, int spawnNumber);

    /**
     * @return the arenas spawn points
     */
    List<Location> getSpawns();

    /**
     * Determines whether given location is within 10 blocks of this arenas spawn points
     *
     * @param loc location to check
     * @return true if given location is within 10 blocks of a spawn, else false
     */
    boolean isCloseToSpawn(Location loc);

    /**
     * Add the specified players to the arena
     *
     * @param players to be added to the arena
     */
    void addPlayers(Player... players);

    /**
     * Called when a plugin reload has occurred
     */
    void reload();
}
