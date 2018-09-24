package de.halfminer.hms.handler.storage;

import de.halfminer.hms.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation via {@link FileConfiguration Bukkit's Yaml config API}.
 */
public class YamlHalfminerPlayer implements HalfminerPlayer {

    private final UUID uuid;

    private final FileConfiguration storage;
    private final String path;

    public YamlHalfminerPlayer(FileConfiguration storage, OfflinePlayer p) {

        this.storage = storage;

        this.uuid = p.getUniqueId();
        this.path = uuid + ".";
    }

    public YamlHalfminerPlayer(FileConfiguration storage, UUID uuid) throws PlayerNotFoundException {

        this.storage = storage;

        this.uuid = uuid;
        this.path = uuid + ".";

        if (getName().length() == 0) {
            throw new PlayerNotFoundException();
        }
    }

    @Override
    public OfflinePlayer getBase() {

        OfflinePlayer player = Bukkit.getPlayer(uuid);
        if (player == null) {
            player = Bukkit.getOfflinePlayer(uuid);
        }

        return player;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public boolean wasSeenBefore() {
        return !getName().isEmpty();
    }

    @Override
    public String getName() {
        return getString(DataType.LAST_NAME);
    }

    @Override
    public List<String> getPreviousNames() {

        String previousNamesStr = getString(DataType.PREVIOUS_NAMES);
        if (previousNamesStr.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> previousNames = Arrays.asList(previousNamesStr.split(" "));

        // filter out current name, if included in list
        String currentName = getName();
        previousNames = previousNames.stream()
                .filter(previousName -> !previousName.equalsIgnoreCase(currentName))
                .collect(Collectors.toList());

        return new ArrayList<>(previousNames);
    }

    @Override
    public int getLevel() {

        OfflinePlayer offlinePlayer = getBase();
        if (!offlinePlayer.isOnline()) {
            throw new RuntimeException("Player " + getName() + " is not online, cannot get level");
        }

        Player player = offlinePlayer.getPlayer();

        int playerLevel = 0;
        while (playerLevel < 6 && player.hasPermission("hms.level." + (playerLevel + 1))) {
            playerLevel++;
        }
        return playerLevel;
    }

    @Override
    public String getIPAddress() {

        OfflinePlayer player = getBase();
        if (!player.isOnline()) {
            throw new RuntimeException("Player " + getName() + " is not online, cannot get IP address");
        }

        // look stupid, but seems to be the most basic way to grab the IP via Bukkit
        return player.getPlayer().getAddress().getAddress().toString().substring(1);
    }

    @Override
    public void set(DataType type, Object setTo) {
        storage.set(path + type, setTo);
    }

    @Override
    public int incrementInt(DataType type, int amount) {
        int newValue = getInt(type) + amount;
        storage.set(path + type, newValue);
        return newValue;
    }

    @Override
    public double incrementDouble(DataType type, double amount) {
        double newValue = getDouble(type) + amount;
        newValue = Utils.roundDouble(newValue);
        storage.set(path + type, newValue);
        return newValue;
    }

    @Override
    public int getInt(DataType type) {
        return storage.getInt(path + type, 0);
    }

    @Override
    public long getLong(DataType type) {
        return storage.getLong(path + type, 0L);
    }

    @Override
    public double getDouble(DataType type) {
        return storage.getDouble(path + type, 0.0d);
    }

    @Override
    public boolean getBoolean(DataType type) {
        return storage.getBoolean(path + type, false);
    }

    @Override
    public String getString(DataType type) {
        return storage.getString(path + type, "");
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof YamlHalfminerPlayer && this.uuid.equals(((YamlHalfminerPlayer) o).uuid);
    }
}
