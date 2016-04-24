package de.halfminer.hms.handlers;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * - Autosave
 * - Flatfiles in .yml format
 *   - Own UUID storage/cache
 *   - Player data storage
 *   - Storage for other types of data
 * - Can easily be queried with YAML API
 * - Thread safe
 */
@SuppressWarnings("unused")
public class HanStorage extends HalfminerHandler implements Disableable {

    private File sysFile;
    private File uuidFile;
    private File playerFile;

    private FileConfiguration sysConfig;
    private FileConfiguration uuidConfig;
    private FileConfiguration playerConfig;

    private BukkitTask task;

    public HanStorage() {
        load();
    }

    public void set(String path, Object value) {
        sysConfig.set(path, value);
    }

    public int incrementInt(String path, int incrementBy) {
        int value = sysConfig.getInt(path, 0) + incrementBy;
        sysConfig.set(path, value);
        return value;
    }

    public Object get(String path) {
        return sysConfig.get(path);
    }

    public String getString(String path) {
        return sysConfig.getString(path, "");
    }

    public int getInt(String path) {
        return sysConfig.getInt(path);
    }

    public long getLong(String path) {
        return sysConfig.getLong(path);
    }

    public void setUUID(OfflinePlayer player) {
        uuidConfig.set(player.getName().toLowerCase(), player.getUniqueId().toString());
    }

    public HalfminerPlayer getPlayer(String playerString) throws PlayerNotFoundException {

        UUID uuid;
        String uuidString = uuidConfig.getString(playerString.toLowerCase(), "");
        if (uuidString.length() > 0) uuid = UUID.fromString(uuidString);
        else {
            Player p = server.getPlayer(playerString);
            if (p != null) {
                uuid = p.getUniqueId();
            } else throw new PlayerNotFoundException();
        }
        return getPlayer(server.getOfflinePlayer(uuid));
    }

    public HalfminerPlayer getPlayer(OfflinePlayer p) {
        return new HalfminerPlayer(playerConfig, p);
    }

    public void saveConfig() {

        try {
            sysConfig.save(sysFile);
            uuidConfig.save(uuidFile);
            playerConfig.save(playerFile);
            hms.getLogger().info(Language.getMessage("hanStorageSaveSuccessful"));
        } catch (IOException e) {
            hms.getLogger().warning(Language.getMessage("hanStorageSaveUnsuccessful"));
            e.printStackTrace();
        }
    }

    public void load() {

        if (sysFile == null) {
            sysFile = new File(hms.getDataFolder(), "sysdata.yml");
            sysConfig = YamlConfiguration.loadConfiguration(sysFile);
        }

        if (uuidFile == null) {
            uuidFile = new File(hms.getDataFolder(), "uuidcache.yml");
            uuidConfig = YamlConfiguration.loadConfiguration(uuidFile);
        }

        if (playerFile == null) {
            playerFile = new File(hms.getDataFolder(), "playerdata.yml");
            playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        }

        int saveInterval = hms.getConfig().getInt("handler.storage.autoSaveMinutes", 15) * 60 * 20;
        if (task != null) task.cancel();
        task = scheduler.runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                saveConfig();
            }
        }, saveInterval, saveInterval);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}