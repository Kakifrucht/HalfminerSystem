package de.halfminer.hmh.data;

import org.bukkit.configuration.ConfigurationSection;

abstract class AbstractHaroPlayer implements HaroPlayer {

    static final String TIME_LEFT_SECONDS_KEY = "timeLeftSeconds";

    private final ConfigurationSection playerStorageRoot;
    private final String playerStorageKey;


    AbstractHaroPlayer(ConfigurationSection playerStorageRoot, String playerStorageKey) {
        this.playerStorageRoot = playerStorageRoot;
        this.playerStorageKey = playerStorageKey;
    }

    @Override
    public String getPlayerStorageKey() {
        return playerStorageKey;
    }

    @Override
    public boolean isAdded() {
        return playerStorageRoot.contains(playerStorageKey);
    }

    ConfigurationSection getPlayerSection() {
        if (!isAdded()) {
            return null;
        }

        return playerStorageRoot.getConfigurationSection(playerStorageKey);
    }
}
