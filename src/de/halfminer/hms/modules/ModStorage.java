package de.halfminer.hms.modules;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

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

    public void setPlayer(Player player, String path, Object value) {
        set(player.getUniqueId().toString() + '.' + path, value);
    }

    public void incrementInt(String path, int incrementBy) {
        fileConfig.set(path, fileConfig.getInt(path, 0) + incrementBy);
    }

    public void incrementPlayerInt(Player player, String path, int incrementBy) {
        incrementInt(player.getUniqueId().toString() + "." + path, incrementBy);
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

    public String getPlayerString(Player player, String path) {
        return getString(player.getUniqueId().toString() + '.' + path);
    }

    public int getPlayerInt(Player player, String path) {
        return getInt(player.getUniqueId().toString() + '.' + path);
    }

    public boolean getPlayerBoolean(Player player, String path) {
        return getBoolean(player.getUniqueId().toString() + '.' + path);
    }

    @Override
    public void reloadConfig() {
        if (file == null) file = new File(hms.getDataFolder(), "storage.yml");
        fileConfig = YamlConfiguration.loadConfiguration(file);

        hms.getServer().getScheduler().runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                onDisable();
            }
        }, 1000, 1000); //TODO use config values
    }

    @Override
    public void onDisable() {
        try {
            fileConfig.save(file);
            hms.getLogger().info("Saving data was successful"); //TODO localization
        } catch (IOException e) {
            hms.getLogger().warning("Saving data was not successful");
            e.printStackTrace();
        }
    }
}
