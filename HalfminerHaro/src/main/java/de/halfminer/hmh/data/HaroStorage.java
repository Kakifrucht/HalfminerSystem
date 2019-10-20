package de.halfminer.hmh.data;

import de.halfminer.hmh.HaroClass;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Reloadable;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Main data class. The plugins config can be accessed through {@link #getHaroConfig()}.
 * The data is backed by a {@link HanStorage} instance.
 *
 * Use {@link #isGameRunning()} to see if the game is currently running, and use {@link #isGameOver()} to check
 * if a winner has been determined already.
 *
 * Returns all added players via {@link #getAddedPlayers(boolean)}, where the parameter specifies if players
 * to be returned should still be alive.
 *
 * All player data is wrapped by a stateless {@link HaroPlayer} instance.
 */
public class HaroStorage extends HaroClass implements Disableable, Reloadable {

    private static final String PLAYER_SECTION_KEY = "players";

    private static final String IS_GAME_RUNNING_KEY = "isRunning";
    private static final String SPAWN_POINT_KEY = "spawn";

    private final HanStorage systemStorage;
    private final HanStorage haroStorage;

    private HaroConfig haroConfig;


    public HaroStorage() {
        this.systemStorage = hms.getStorageHandler();
        this.haroStorage = new HanStorage(hmh);
        this.haroStorage.loadConfig();

        this.haroConfig = new HaroConfig(hmh.getConfig());
    }

    public void storeDataOnDisk() {
        haroStorage.saveConfig();
    }

    /**
     * @return this plugins config, instances should not be kept around, as they are completely
     *          discarded after a plugin reload
     */
    public HaroConfig getHaroConfig() {
        return haroConfig;
    }

    public boolean isGameRunning() {
        return haroStorage.getRootSection().getBoolean(IS_GAME_RUNNING_KEY, false);
    }

    public boolean isGameOver() {
        return isGameRunning() && getAddedPlayers(true).size() <= 1;
    }

    public void setGameRunning(boolean isRunning) {
        haroStorage.set(IS_GAME_RUNNING_KEY, isRunning);
    }

    public List<HaroPlayer> getAddedPlayers(boolean isNotEliminated) {

        List<HaroPlayer> players = new ArrayList<>();
        for (String playerKey : haroStorage.getConfigurationSection(PLAYER_SECTION_KEY).getKeys(false)) {

            try {
                HalfminerPlayer halfminerPlayer = systemStorage.getPlayer(UUID.fromString(playerKey));
                HaroPlayer haroPlayer = getHaroPlayer(halfminerPlayer);
                if (!isNotEliminated || !haroPlayer.isEliminated()) {
                    players.add(haroPlayer);
                }

            } catch (PlayerNotFoundException e) {
                hmh.getLogger().warning("Already added player with UUID '" + playerKey + "' not found in system storage, skipping");
            } catch (IllegalArgumentException ignored) {
                // thrown by UUID.fromString() call, if not a UUID
                players.add(getHaroPlayer(playerKey));
            }
        }

        return players;
    }

    /**
     * Get player by username.
     *
     * @param name username of player, must be a valid username
     * @throws IllegalArgumentException if parameter name is not a valid username
     * @return if player is known a {@link UUIDHaroPlayer}, else a {@link NameHaroPlayer} instance
     */
    public HaroPlayer getHaroPlayer(String name) {

        if (Utils.filterNonUsernameChars(name).length() != name.length()) {
            throw new IllegalArgumentException("Supplied argument is not a valid username: " + name);
        }

        try {
            HalfminerPlayer halfminerPlayer = systemStorage.getPlayer(name);
            return getHaroPlayer(halfminerPlayer);
        } catch (PlayerNotFoundException e) {
            return new NameHaroPlayer(name, haroStorage.getConfigurationSection(PLAYER_SECTION_KEY));
        }
    }

    public HaroPlayer getHaroPlayer(Player player) {
        return getHaroPlayer(systemStorage.getPlayer(player));
    }

    public HaroPlayer getHaroPlayer(HalfminerPlayer hPlayer) {

        ConfigurationSection playerSection = haroStorage.getConfigurationSection(PLAYER_SECTION_KEY);
        HaroPlayer uuidHaroPlayer = new UUIDHaroPlayer(hPlayer, playerSection);

        // check if player was added by username, migrate to UUID if necessary
        HaroPlayer haroPlayerByUsername = new NameHaroPlayer(hPlayer.getName(), playerSection);
        if (haroPlayerByUsername.isAdded()) {
            ConfigurationSection sectionToCopy = getPlayerSection().getConfigurationSection(haroPlayerByUsername.getPlayerStorageKey());
            getPlayerSection().set(uuidHaroPlayer.getPlayerStorageKey(), sectionToCopy);
            removePlayer(haroPlayerByUsername);
        }

        return uuidHaroPlayer;
    }

    public boolean addPlayer(HaroPlayer haroPlayer) {
        if (haroPlayer.isAdded()) {
            return false;
        }

        getPlayerSection().createSection(haroPlayer.getPlayerStorageKey());
        return true;
    }

    public boolean removePlayer(HaroPlayer haroPlayer) {
        if (!haroPlayer.isAdded()) {
            return false;
        }

        getPlayerSection().set(haroPlayer.getPlayerStorageKey(), null);
        return true;
    }

    /**
     * Removes all added players.
     */
    public void removeAllPlayers() {
        haroStorage.set(PLAYER_SECTION_KEY, null);
    }

    public Location getSpawnPoint() {
        if (!haroStorage.getRootSection().contains(SPAWN_POINT_KEY)) {
            return null;
        }

        return (Location) haroStorage.getRootSection().get(SPAWN_POINT_KEY);
    }

    public void setSpawnPoint(Location location) {
        haroStorage.set(SPAWN_POINT_KEY, location);
    }

    @Override
    public void loadConfig() {
        this.haroConfig = new HaroConfig(hmh.getConfig());
    }

    @Override
    public void onDisable() {

        for (HaroPlayer addedPlayer : getAddedPlayers(false)) {
            if (addedPlayer.isOnline()) {
                addedPlayer.setOffline();
            }
        }

        storeDataOnDisk();
    }

    private ConfigurationSection getPlayerSection() {

        if (!haroStorage.getRootSection().contains(PLAYER_SECTION_KEY)) {
            haroStorage.getRootSection().createSection(PLAYER_SECTION_KEY);
        }

        return haroStorage.getRootSection().getConfigurationSection(PLAYER_SECTION_KEY);
    }
}
