package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.type.DataType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

/**
 * Object to access stored player information
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
public class HalfminerPlayer {

    private final UUID uuid;
    private OfflinePlayer player = null;

    private final FileConfiguration storage;
    private final String path;

    public HalfminerPlayer(FileConfiguration storage, OfflinePlayer p) {

        this.storage = storage;

        this.uuid = p.getUniqueId();
        this.player = p;
        this.path = uuid + ".";
    }

    public HalfminerPlayer(FileConfiguration storage, UUID uuid) throws PlayerNotFoundException {

        this.storage = storage;

        this.uuid = uuid;
        this.path = uuid + ".";

        if (getName().length() == 0) {
            throw new PlayerNotFoundException();
        }
    }

    public OfflinePlayer getBase() {

        if (player == null) {
            player = HalfminerSystem.getInstance().getServer().getOfflinePlayer(uuid);
        }
        return player;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return getString(DataType.LAST_NAME);
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
        return o instanceof HalfminerPlayer && this.uuid.equals(((HalfminerPlayer) o).uuid);
    }
}
