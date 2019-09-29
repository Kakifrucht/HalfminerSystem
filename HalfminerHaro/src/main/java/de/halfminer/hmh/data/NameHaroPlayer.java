package de.halfminer.hmh.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/**
 * Player where only the name is known. Used when a player hasn't joined the server before.
 */
public class NameHaroPlayer extends YAMLHaroPlayer {

    private final String name;


    NameHaroPlayer(String name, ConfigurationSection playerStorageRoot) {
        super(playerStorageRoot, name.toLowerCase());
        if (name.length() == 0) {
            throw new IllegalArgumentException("Tried to create a NameHaroPlayer with empty name");
        }

        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOnline() {
        return false;
    }

    @Override
    public Player getBase() {
        return null;
    }

    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public void setInitialized(boolean initialized) {
        throw new UnsupportedOperationException("Cannot set the initialization state of NameHaroPlayer instance");
    }

    @Override
    public boolean isEliminated() {
        return false;
    }

    @Override
    public void setEliminated(boolean eliminated) {
        throw new UnsupportedOperationException("Cannot set elimination state of NameHaroPlayer instance");
    }

    @Override
    public boolean hasTimeLeft() {
        return getTimeLeftSeconds() > 0;
    }

    @Override
    public int getTimeLeftSeconds() {
        return getPlayerSection().getInt(TIME_LEFT_SECONDS_KEY, 0);
    }

    @Override
    public void setTimeLeftSeconds(int timeLeftSeconds) {
        getPlayerSection().set(TIME_LEFT_SECONDS_KEY, timeLeftSeconds);
    }

    @Override
    public void setTimeUntilKick(long timeUntilKick) {
        throw new UnsupportedOperationException("Cannot set time until kick of NameHaroPlayer instance");
    }

    @Override
    public void setOffline() {
        throw new UnsupportedOperationException("Cannot set online state of NameHaroPlayer instance");
    }
}
