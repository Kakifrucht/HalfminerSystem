package de.halfminer.hmh.data;

import de.halfminer.hms.handler.storage.HalfminerPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Class represents a player that can be part of a game. Doesn't hold any state, objects don't have to be kept around.
 * Instead a new one is created on demand. Storage is handled through Bukkit's YAML API.
 */
public class UUIDHaroPlayer extends YAMLHaroPlayer {

    private static final String TIMESTAMP_UNTIL_KICK = "timestampUntilKick";
    private static final String INITIALIZED_KEY = "initialized";
    private static final String IS_ELIMINATED_KEY = "isEliminated";

    private final HalfminerPlayer player;


    UUIDHaroPlayer(HalfminerPlayer player, ConfigurationSection playerStorageRoot) {
        super(playerStorageRoot, player.getUniqueId().toString());
        this.player = player;
    }

    @Override
    public String getName() {
        return player.getName();
    }

    @Override
    public boolean isOnline() {
        return player.getBase().isOnline();
    }

    @Override
    public Player getBase() {
        if (!isOnline()) {
            return null;
        }

        return player.getBase().getPlayer();
    }

    @Override
    public boolean isInitialized() {
        if (!isAdded()) {
            return false;
        }

        return getPlayerSection().getBoolean(INITIALIZED_KEY, false);
    }

    @Override
    public void setInitialized(boolean initialized) {
        getPlayerSection().set(INITIALIZED_KEY, initialized);
    }

    @Override
    public boolean isEliminated() {
        if (!isAdded()) {
            return false;
        }

        return getPlayerSection().getBoolean(IS_ELIMINATED_KEY, false);
    }

    @Override
    public void setEliminated(boolean eliminated) {
        getPlayerSection().set(IS_ELIMINATED_KEY, eliminated);
    }

    @Override
    public boolean hasTimeLeft() {
        return getTimeLeftSeconds() > 0;
    }

    @Override
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

    @Override
    public void setTimeLeftSeconds(int timeLeftSeconds) {
        if (isAdded()) {
            if (isOnline()) {
                getPlayerSection().set(TIMESTAMP_UNTIL_KICK, (System.currentTimeMillis() / 1000L) + timeLeftSeconds);
            } else {
                getPlayerSection().set(TIME_LEFT_SECONDS_KEY, timeLeftSeconds);
            }
        }
    }

    @Override
    public void setTimeUntilKick(long timeUntilKick) {
        if (isAdded()) {
            getPlayerSection().set(TIMESTAMP_UNTIL_KICK, timeUntilKick);
        }
    }

    @Override
    public void setOffline() {

        if (!isAdded()) {
            return;
        }

        ConfigurationSection playerSection = getPlayerSection();
        playerSection.set(TIME_LEFT_SECONDS_KEY, getTimeLeftSeconds());
        playerSection.set(TIMESTAMP_UNTIL_KICK, null);
    }
}
