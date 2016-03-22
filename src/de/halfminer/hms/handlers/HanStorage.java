package de.halfminer.hms.handlers;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.StatsType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.util.Language;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Stores all information into own flatfile
 * - Autosave
 * - Flatfile in .yml format
 * - Can easily be queried with YAML API
 * - Thread safe
 */
@SuppressWarnings("ALL")
public class HanStorage extends HalfminerHandler implements Disableable {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    private File file;
    private FileConfiguration fileConfig;
    private int taskId;

    public HanStorage() {
        load();
    }

    public void set(String path, Object value) {
        fileConfig.set(path, value);
    }

    public int incrementInt(String path, int incrementBy) {
        int value = fileConfig.getInt(path, 0) + incrementBy;
        fileConfig.set(path, value);
        return value;
    }

    public double incrementDouble(String path, double incrementBy) {
        double value = fileConfig.getDouble(path, 0.0d) + incrementBy;
        value = Math.round(value * 100) / 100.0d;
        fileConfig.set(path, value);
        return value;
    }

    public void setUUID(OfflinePlayer player) {
        set("sys.uuid." + player.getName().toLowerCase(), player.getUniqueId().toString());
    }

    public void setStats(OfflinePlayer player, StatsType stats, Object value) {
        set(player.getUniqueId().toString() + '.' + stats, value);
    }

    public Object get(String path) {
        return fileConfig.get(path);
    }

    public String getString(String path) {
        return fileConfig.getString(path, "");
    }

    public int getInt(String path) {
        return fileConfig.getInt(path);
    }

    public long getLong(String path) {
        return fileConfig.getLong(path);
    }

    public double getDouble(String path) {
        return fileConfig.getDouble(path);
    }

    public boolean getBoolean(String path) {
        return fileConfig.getBoolean(path);
    }

    public UUID getUUID(String playerName) throws PlayerNotFoundException {
        String uuidString = getString("sys.uuid." + playerName.toLowerCase());
        if (uuidString.length() > 0) return UUID.fromString(uuidString);
        else {
            Player p = server.getPlayer(playerName);
            if (p != null) {
                return p.getUniqueId();
            } else throw new PlayerNotFoundException();
        }
    }

    public String getStatsString(OfflinePlayer player, StatsType stats) {
        return getString(player.getUniqueId() + "." + stats);
    }

    public int getStatsInt(OfflinePlayer player, StatsType stats) {
        return getInt(player.getUniqueId() + "." + stats);
    }

    public double getStatsDouble(OfflinePlayer player, StatsType stats) {
        return getDouble(player.getUniqueId() + "." + stats);
    }

    public boolean getStatsBoolean(OfflinePlayer player, StatsType stats) {
        return getBoolean(player.getUniqueId() + "." + stats);
    }

    public int incrementStatsInt(OfflinePlayer player, StatsType stats, int incrementBy) {
        return incrementInt(player.getUniqueId() + "." + stats, incrementBy);
    }

    public double incrementStatsDouble(OfflinePlayer player, StatsType stats, double incrementBy) {
        return incrementDouble(player.getUniqueId() + "." + stats, incrementBy);
    }

    public void saveConfig() {

        try {
            fileConfig.save(file);
            hms.getLogger().info(Language.getMessage("hanStorageSaveSuccessful"));
        } catch (IOException e) {
            hms.getLogger().warning(Language.getMessage("hanStorageSaveUnsuccessful"));
            e.printStackTrace();
        }
    }

    public void load() {

        if (file == null) {
            file = new File(hms.getDataFolder(), "storage.yml");
            fileConfig = YamlConfiguration.loadConfiguration(file);
        }

        int saveInterval = hms.getConfig().getInt("storage.autoSaveMinutes", 15) * 60 * 20;
        if (taskId > 0) scheduler.cancelTask(taskId);
        taskId = scheduler.runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                saveConfig();
            }
        }, saveInterval, saveInterval).getTaskId();
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
