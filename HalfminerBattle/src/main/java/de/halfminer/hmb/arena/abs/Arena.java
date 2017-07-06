package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.mode.abs.BattleMode;
import de.halfminer.hmb.mode.abs.BattleModeType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Arenas are being managed by {@link de.halfminer.hmb.data.ArenaManager}
 */
public interface Arena {

    /**
     * @return true if arena is configured, currently free and thus can be entered
     */
    boolean isFree();

    /**
     * @return the arenas name
     */
    String getName();

    /**
     * @return amount of player currently in arena
     */
    int getPlayerCount();

    BattleMode getBattleMode();

    /**
     * @return the arenas battle mode
     */
    BattleModeType getBattleModeType();

    /**
     * @return players currently in arena
     */
    List<Player> getPlayersInArena();

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
     * Deletes the specified spawn, or clears it, if it is out of bounds.
     *
     * @param spawnNumber spawn to remove (0 is first spawn etc.)
     */
    void removeSpawn(int spawnNumber);

    /**
     * @return the arenas spawn points
     */
    List<Location> getSpawns();

    /**
     * @return the arenas current kit, or null if none defined
     */
    ItemStack[] getKit();

    /**
     * @param kit to set for the arena
     */
    void setKit(ItemStack[] kit);

    /**
     * Determines whether given location is within 10 blocks of this arenas spawn points
     *
     * @param loc location to check
     * @return true if given location is within 10 blocks of a spawn, else false
     */
    boolean isCloseToSpawn(Location loc);

    /**
     * Removes all players from the arena
     */
    boolean forceGameEnd();
}
