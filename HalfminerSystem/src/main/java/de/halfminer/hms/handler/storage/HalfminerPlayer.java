package de.halfminer.hms.handler.storage;

import org.bukkit.OfflinePlayer;

import java.util.UUID;

/**
 * Interface to access stored player information {@link DataType}.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
public interface HalfminerPlayer {

    OfflinePlayer getBase();

    UUID getUniqueId();

    boolean wasSeenBefore();

    String getName();

    int getLevel();

    String getIPAddress();

    void set(DataType type, Object setTo);

    int incrementInt(DataType type, int amount);

    double incrementDouble(DataType type, double amount);

    int getInt(DataType type);

    long getLong(DataType type);

    double getDouble(DataType type);

    boolean getBoolean(DataType type);

    String getString(DataType type);
}
