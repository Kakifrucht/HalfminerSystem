package de.halfminer.hms.handlers;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.interfaces.CacheHolder;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.interfaces.Reloadable;
import de.halfminer.hms.util.CustomtextCache;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * - Autosave
 * - Flatfiles in .yml format
 *   - Own UUID storage/cache
 *   - Player data storage
 *   - Storage for other types of data
 * - Can easily be queried with YAML API
 * - Caches customtext files
 *   - To mark a chapter, use "#chaptername argument" (argument optional and not limited, case insensitive)
 *     - Supports aliases via comma such as "#chaptername argument,alias argument"
 *     - Supports wildcards, such as "#chaptername argument *" or "#chaptername *"
 *     - Supports aliases in between via '|' char, such as #chapter subchapter|subchapteralias
 *   - Automatic replacement of '&' with Bukkit color code
 *   - If line ends with space char, add next line to current line
 * - Thread safe
 */
@SuppressWarnings({"unused", "SameParameterValue"})
public class HanStorage extends HalfminerHandler implements CacheHolder, Disableable, Reloadable {

    private File sysFile;
    private File uuidFile;
    private File playerFile;

    private FileConfiguration sysConfig;
    private FileConfiguration uuidConfig;
    private FileConfiguration playerConfig;

    private final Map<File, CustomtextCache> textCaches = new HashMap<>();

    private BukkitTask task;

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
        return getPlayer(uuid);
    }

    public HalfminerPlayer getPlayer(OfflinePlayer p) {
        return new HalfminerPlayer(playerConfig, p);
    }

    public HalfminerPlayer getPlayer(UUID uuid) {
        return new HalfminerPlayer(playerConfig, uuid);
    }

    public CustomtextCache getCache(String fileName) throws CachingException {

        File cacheFile = new File(hms.getDataFolder(), fileName);

        if (!cacheFile.exists()) {

            // check jar first if it has such file
            if (hms.getResource(fileName) != null) hms.saveResource(fileName, false);
            else {
                try {
                    //noinspection ResultOfMethodCallIgnored - we'll check below
                    cacheFile.createNewFile();
                } catch (IOException ignored) {
                }
            }

            if (cacheFile.exists()) {
                MessageBuilder.create(hms, "hanStorageCacheCreate")
                        .addPlaceholderReplace("%FILENAME%", cacheFile.getName())
                        .logMessage(Level.INFO);
            } else {
                MessageBuilder.create(hms, "hanStorageCacheCouldNotCreate").logMessage(Level.SEVERE);
                throw new CachingException(fileName, CachingException.Reason.CANNOT_WRITE);
            }
        }

        if (textCaches.containsKey(cacheFile))
            return textCaches.get(cacheFile);

        CustomtextCache cache = new CustomtextCache(cacheFile);
        textCaches.put(cacheFile, cache);
        return cache;
    }

    public void saveConfig() {

        try {
            sysConfig.save(sysFile);
            uuidConfig.save(uuidFile);
            playerConfig.save(playerFile);
            MessageBuilder.create(hms, "hanStorageSaveSuccessful").logMessage(Level.INFO);
        } catch (IOException e) {
            MessageBuilder.create(hms, "hanStorageSaveUnsuccessful").logMessage(Level.WARNING);
            e.printStackTrace();
        }
    }

    @Override
    public void loadConfig() {

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
        task = scheduler.runTaskTimerAsynchronously(hms, this::saveConfig, saveInterval, saveInterval);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
