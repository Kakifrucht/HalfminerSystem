package de.halfminer.hmh.data;

import org.bukkit.configuration.ConfigurationSection;

/**
 * Implementations where data is backed by a YAML {@link ConfigurationSection}.
 * Use {@link #getPlayerSection()} to access the players data section.
 */
abstract class YAMLHaroPlayer implements HaroPlayer {

    static final String TIME_LEFT_SECONDS_KEY = "timeLeftSeconds";

    private final ConfigurationSection playerStorageRoot;
    private final String playerStorageKey;


    YAMLHaroPlayer(ConfigurationSection playerStorageRoot, String playerStorageKey) {
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
