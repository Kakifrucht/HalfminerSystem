package de.halfminer.hmh.data;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Interface represents a player that can be part of a game.
 */
public interface HaroPlayer {

    String getName();

    boolean isOnline();

    Player getBase();

    OfflinePlayer getOfflinePlayer();

    /**
     * @return true if player has been added to the game, else false
     */
    boolean isAdded();

    /**
     * Check if player was initialized for the current game.
     *
     * @return true if player was initialized before for the current game, else false
     */
    boolean isInitialized();

    void setInitialized(boolean initialized);

    boolean isEliminated();

    void setEliminated(boolean eliminated);

    /**
     * @return true if the player still has time left to play
     */
    boolean hasTimeLeft();

    /**
     * Get the time in seconds the player has left. If used on an offline player it will read the remaining time from
     * database, if the player is online, set timestamp until kick via {@link #setTimeUntilKick(long)}.
     *
     * @return time in seconds the player has left,
     */
    int getTimeLeftSeconds();

    void setTimeLeftSeconds(int timeLeftSeconds);

    /**
     * Set the time, after passing the time the player should be kicked.
     * Will be taken into account when calling {@link #getTimeLeftSeconds()}.
     *
     * {@link #setOffline()} needs to be called once this player leaves.
     *
     * @param timeUntilKick timestamp in seconds
     */
    void setTimeUntilKick(long timeUntilKick);

    /**
     * Marks this player offline to correctly set the time left remaining returned by {@link #getTimeLeftSeconds()}.
     * Needs to be called if {@link #setTimeUntilKick(long)} was used on this player.
     */
    void setOffline();

    /**
     * Set the players last known health.
     *
     * @param currentHealth health points the player should have
     * @param maxHealth maximum health of player
     */
    void setHealth(double currentHealth, double maxHealth);

    /**
     * @return health points of player
     */
    double getCurrentHealth();

    /**
     * @return maximum health of player
     */
    double getMaxHealth();

    /**
     * @return the players yaml storage key
     */
    String getPlayerStorageKey();
}
