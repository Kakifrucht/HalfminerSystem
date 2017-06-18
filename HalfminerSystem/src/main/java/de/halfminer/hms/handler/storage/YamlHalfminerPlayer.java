package de.halfminer.hms.handler.storage;

import de.halfminer.hms.exceptions.PlayerNotFoundException;
import de.halfminer.hms.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Implementation via {@link FileConfiguration Bukkit's Yaml config API}.
 */
public class YamlHalfminerPlayer implements HalfminerPlayer {

    private final UUID uuid;
    private OfflinePlayer player = null;

    private final FileConfiguration storage;
    private final String path;

    public YamlHalfminerPlayer(FileConfiguration storage, OfflinePlayer p) {

        this.storage = storage;

        this.uuid = p.getUniqueId();
        this.player = p;
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

    public OfflinePlayer getBase() {

        if (player == null) {
            player = Bukkit.getOfflinePlayer(uuid);
        }
        return player;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return getString(DataType.LAST_NAME);
    }

    public int getLevel() {
        if (!player.isOnline())
            throw new RuntimeException("Player " + getName() + " is not online, cannot get level");

        Player p = player.getPlayer();

        int playerLevel = 0;
        while (playerLevel < 6 && p.hasPermission("hms.level." + (playerLevel + 1))) {
            playerLevel++;
        }
        return playerLevel;
    }

    public String getIPAddress() {
        if (!player.isOnline())
            throw new RuntimeException("Player " + getName() + " is not online, cannot get IP address");

        // look stupid, but seems to be the most basic way to grab the IP via Bukkit
        return player.getPlayer().getAddress().getAddress().toString().substring(1);
    }

    public void set(DataType type, Object setTo) {
        storage.set(path + type, setTo);
    }

    public int incrementInt(DataType type, int amount) {
        int newValue = getInt(type) + amount;
        storage.set(path + type, newValue);
        return newValue;
    }

    public double incrementDouble(DataType type, double amount) {
        double newValue = getDouble(type) + amount;
        newValue = Utils.roundDouble(newValue);
        storage.set(path + type, newValue);
        return newValue;
    }

    public int getInt(DataType type) {
        return storage.getInt(path + type, 0);
    }

    public long getLong(DataType type) {
        return storage.getLong(path + type, 0L);
    }

    public double getDouble(DataType type) {
        return storage.getDouble(path + type, 0.0d);
    }

    public boolean getBoolean(DataType type) {
        return storage.getBoolean(path + type, false);
    }

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
