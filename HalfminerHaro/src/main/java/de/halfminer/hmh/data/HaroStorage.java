package de.halfminer.hmh.data;

import de.halfminer.hmh.HaroClass;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Reloadable;
import org.bukkit.Location;
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
 * In order to set a players current time remaining, {@link #playerJoined(HaroPlayer)} must be called after join event.
 * Initialize players by calling {@link #initializePlayer(Player)}. This method should only be called once per game.
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

        // set players online, if plugin was completely reloaded and players are already online
        for (Player onlinePlayer : server.getOnlinePlayers()) {
            playerJoined(getHaroPlayer(onlinePlayer));
        }
    }

    public void storeDataOnDisk() {
        haroStorage.saveConfig();
    }

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

    public List<HaroPlayer> getAddedPlayers(boolean isAlive) {

        List<HaroPlayer> players = new ArrayList<>();
        for (String uuid : haroStorage.getConfigurationSection(PLAYER_SECTION_KEY).getKeys(false)) {
            try {
                HalfminerPlayer halfminerPlayer = systemStorage.getPlayer(UUID.fromString(uuid));
                HaroPlayer haroPlayer = getHaroPlayer(halfminerPlayer);
                if (!isAlive || !haroPlayer.isDead()) {
                    players.add(haroPlayer);
                }

            } catch (PlayerNotFoundException e) {
                hmh.getLogger().warning("Already added player with UUID '" + uuid + "' not found in system storage, skipping");
            }
        }

        return players;
    }

    public HaroPlayer getHaroPlayer(Player player) {
        return getHaroPlayer(systemStorage.getPlayer(player));
    }

    public HaroPlayer getHaroPlayer(HalfminerPlayer hPlayer) {
        return new HaroPlayer(hPlayer, haroStorage.getConfigurationSection(PLAYER_SECTION_KEY));
    }

    public void playerJoined(HaroPlayer haroPlayer) {

        if (!haroPlayer.isAdded()) {
            return;
        }

        if (isGameRunning()) {
            haroPlayer.setTimeUntilKick(System.currentTimeMillis() / 1000L + haroPlayer.getTimeLeftSeconds());
        }
    }

    public boolean addPlayer(HaroPlayer haroPlayer) {
        if (haroPlayer.isAdded()) {
            return false;
        }

        haroStorage.getRootSection().createSection(PLAYER_SECTION_KEY + '.' + haroPlayer.getUniqueId().toString());
        return true;
    }

    public boolean removePlayer(HaroPlayer haroPlayer) {
        if (!haroPlayer.isAdded()) {
            return false;
        }

        haroStorage.getRootSection().set(PLAYER_SECTION_KEY + '.' + haroPlayer.getUniqueId().toString(), null);
        return true;
    }

    public void removeAllPlayers() {
        haroStorage.set(PLAYER_SECTION_KEY, null);
    }

    public void initializePlayer(Player player) {

        HaroPlayer haroPlayer = getHaroPlayer(player);
        List<String> playerInitCommands = haroConfig.getPlayerInitCommands();
        for (String playerInitCommand : playerInitCommands) {
            String command = playerInitCommand.replace("%PLAYER%", haroPlayer.getName());
            server.dispatchCommand(server.getConsoleSender(), command);
        }

        // check distance to spawn, teleport if too far away
        Location spawnPoint = getSpawnPoint();
        if (spawnPoint != null) {
            double maxDistance = haroConfig.getMaxSpawnDistance();
            if (spawnPoint.distance(player.getLocation()) > maxDistance) {
                player.teleport(spawnPoint);
            }
        }

        haroPlayer.setTimeUntilKick(System.currentTimeMillis() / 1000L + haroPlayer.getTimeLeftSeconds());
        haroPlayer.setInitialized(true);
        hmh.getLogger().info("Player " + player.getName() + " was initialized");
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
}
