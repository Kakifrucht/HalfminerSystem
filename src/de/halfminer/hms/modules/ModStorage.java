package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ModStorage extends HalfminerModule {

    private File file;
    private FileConfiguration fileConfig;

    public ModStorage() {
        reloadConfig();
    }

    public void set(String path, Object value) {
        fileConfig.set(path, value);
    }

    public void setPlayer(OfflinePlayer player, String path, Object value) {
        set(player.getUniqueId().toString() + '.' + path, value);
    }

    public void incrementInt(String path, int incrementBy) {
        fileConfig.set(path, fileConfig.getInt(path, 0) + incrementBy);
    }

    public void incrementPlayerInt(OfflinePlayer player, String path, int incrementBy) {
        incrementInt(player.getUniqueId().toString() + "." + path, incrementBy);
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

    public boolean getBoolean(String path) {
        return fileConfig.getBoolean(path);
    }

    public String getPlayerString(OfflinePlayer player, String path) {
        return getString(player.getUniqueId().toString() + '.' + path);
    }

    public int getPlayerInt(OfflinePlayer player, String path) {
        return getInt(player.getUniqueId().toString() + '.' + path);
    }

    public boolean getPlayerBoolean(OfflinePlayer player, String path) {
        return getBoolean(player.getUniqueId().toString() + '.' + path);
    }

    @Override
    public void reloadConfig() {
        if (file == null) file = new File(hms.getDataFolder(), "storage.yml");
        fileConfig = YamlConfiguration.loadConfiguration(file);

        int saveInterval = hms.getConfig().getInt("storage.autoSaveMinutes", 15) * 60 * 20;
        hms.getServer().getScheduler().runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                onDisable();
            }
        }, saveInterval, saveInterval);
    }

    @Override
    public void onDisable() {
        try {
            fileConfig.save(file);
            hms.getLogger().info(Language.getMessage("modStorageSaveSuccessful"));
        } catch (IOException e) {
            hms.getLogger().warning(Language.getMessage("modStorageSaveUnsuccessful"));
            e.printStackTrace();
        }
    }
}
