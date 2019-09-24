package de.halfminer.hmh.data;

import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Class represents a player that can be part of a game. Doesn't hold any state, objects don't have to be kept around.
 * Instead a new one is created on demand. Storage is handled through Bukkit's YAML API.
 */
public class HaroPlayer {

    private static final String TIME_LEFT_SECONDS_KEY = "timeLeftSeconds";
    private static final String TIMESTAMP_UNTIL_KICK = "timestampUntilKick";
    private static final String INITIALIZED_KEY = "initialized";
    private static final String IS_DEAD_KEY = "isDead";

    private final HalfminerPlayer player;
    private final ConfigurationSection haroStorageRoot;


    HaroPlayer(HalfminerPlayer player, ConfigurationSection haroStorageRoot) {
        this.player = player;
        this.haroStorageRoot = haroStorageRoot;
    }

    public String getName() {
        return player.getName();
    }

    public boolean isOnline() {
        return player.getBase().isOnline();
    }

    public Player getBase() {
        if (!isOnline()) {
            return null;
        }

        return player.getBase().getPlayer();
    }

    UUID getUniqueId() {
        return player.getUniqueId();
    }

    public boolean isAdded() {
        return haroStorageRoot.contains(player.getUniqueId().toString());
    }

    /**
     * Check if player was initialized for the current game.
     *
     * @return true if player was initialized before for the current game, else false
     */
    public boolean isInitialized() {
        if (!isAdded()) {
            return false;
        }

        return getPlayerSection().getBoolean(INITIALIZED_KEY, false);
    }

    public void setInitialized(boolean initialized) {
        getPlayerSection().set(INITIALIZED_KEY, initialized);
    }

    public boolean isDead() {
        if (!isAdded()) {
            return false;
        }

        return getPlayerSection().getBoolean(IS_DEAD_KEY, false);
    }

    public void setDead(boolean dead) {
        getPlayerSection().set(IS_DEAD_KEY, dead);
    }

    /**
     * @return true if the player still has time left to play
     */
    public boolean hasTimeLeft() {
        return getTimeLeftSeconds() > 0;
    }

    public int getTimeLeftSeconds() {
        if (isAdded()) {

            ConfigurationSection playerSection = getPlayerSection();
            if (isOnline() && playerSection.contains(TIMESTAMP_UNTIL_KICK)) {
                long timestampUntilKick = playerSection.getLong(TIMESTAMP_UNTIL_KICK);
                return (int) (timestampUntilKick - (System.currentTimeMillis() / 1000L));
            }

            return playerSection.getInt(TIME_LEFT_SECONDS_KEY, 0);
        }

        return 0;
    }

    public void setTimeLeftSeconds(int timeLeftSeconds) {
        if (isAdded()) {
            if (isOnline()) {
                getPlayerSection().set(TIMESTAMP_UNTIL_KICK, (System.currentTimeMillis() / 1000L) + timeLeftSeconds);
            } else {
                getPlayerSection().set(TIME_LEFT_SECONDS_KEY, timeLeftSeconds);
            }
        }
    }

    void setTimeUntilKick(long timeUntilKick) {
        if (isAdded()) {
            getPlayerSection().set(TIMESTAMP_UNTIL_KICK, timeUntilKick);
        }
    }

    /**
     * Marks this player offline to correctly set the time left remaining returned by {@link #getTimeLeftSeconds()}.
     */
    public void setOffline() {

        if (!isAdded()) {
            return;
        }

        ConfigurationSection playerSection = getPlayerSection();
        playerSection.set(TIME_LEFT_SECONDS_KEY, getTimeLeftSeconds());
        playerSection.set(TIMESTAMP_UNTIL_KICK, null);
    }

    private ConfigurationSection getPlayerSection() {
        if (!isAdded()) {
            return null;
        }

        return haroStorageRoot.getConfigurationSection(player.getUniqueId().toString());
    }
}
