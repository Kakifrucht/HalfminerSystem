package de.halfminer.hms.util;

import de.halfminer.hms.enums.DataType;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Object to access stored player information
 */
public class HalfminerPlayer {

    final FileConfiguration storage;
    final String path;

    public HalfminerPlayer(FileConfiguration storage, OfflinePlayer p) {
        this.storage = storage;
        this.path = p.getUniqueId() + ".";
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
        storage.set(path + type, newValue);
        return newValue;
    }

    public int getInt(DataType type) {
        return storage.getInt(path + type, 0);
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
}
