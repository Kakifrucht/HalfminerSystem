package de.halfminer.hms;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class HalfminerStorage {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    private File file;
    private FileConfiguration fileConfig;
    private int taskId;

    public HalfminerStorage() {
        reloadConfig();
    }

    public void set(String path, Object value) {
        fileConfig.set(path, value);
    }

    public int incrementInt(String path, int incrementBy) {
        int value = fileConfig.getInt(path, 0) + incrementBy;
        fileConfig.set(path, value);
        return value;
    }

    public void setUUID(String playerName, UUID uid) {
        set("uid." + playerName.toLowerCase(), uid.toString());
    }

    public void setStats(OfflinePlayer player, StatsType stats, Object value) {
        set(player.getUniqueId().toString() + '.' + stats, value);
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
        String uuidString = getString("uid." + playerName.toLowerCase());
        if (uuidString.length() > 0) return UUID.fromString(uuidString);
        else throw new PlayerNotFoundException();
    }

    public String getStatsString(OfflinePlayer player, StatsType stats) {
        return getString(player.getUniqueId().toString() + '.' + stats);
    }

    public int getStatsInt(OfflinePlayer player, StatsType stats) {
        return getInt(player.getUniqueId().toString() + '.' + stats);
    }

    public double getStatsDouble(OfflinePlayer player, StatsType stats) {
        return getDouble(player.getUniqueId().toString() + '.' + stats);
    }

    public boolean getStatsBoolean(OfflinePlayer player, StatsType stats) {
        return getBoolean(player.getUniqueId().toString() + '.' + stats);
    }

    public void saveConfig() {
        try {
            fileConfig.save(file);
            hms.getLogger().info(Language.getMessage("modStorageSaveSuccessful"));
        } catch (IOException e) {
            hms.getLogger().warning(Language.getMessage("modStorageSaveUnsuccessful"));
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        if (file == null) {
            file = new File(hms.getDataFolder(), "storage.yml");
            fileConfig = YamlConfiguration.loadConfiguration(file);
        }

        int saveInterval = hms.getConfig().getInt("storage.autoSaveMinutes", 15) * 60 * 20;
        if (taskId > 0) hms.getServer().getScheduler().cancelTask(taskId);
        taskId = hms.getServer().getScheduler().runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                saveConfig();
            }
        }, saveInterval, saveInterval).getTaskId();
    }
}
