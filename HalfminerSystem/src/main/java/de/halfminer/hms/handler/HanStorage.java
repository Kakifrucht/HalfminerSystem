package de.halfminer.hms.handler;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.cache.CacheHolder;
import de.halfminer.hms.cache.CustomtextCache;
import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.handler.storage.StorageListener;
import de.halfminer.hms.handler.storage.YamlHalfminerPlayer;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Reloadable;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * - Data stored in YAML flatfiles
 *   - Player data storage
 *     - Collects default information about every player
 *       - Online time
 *       - Last login time
 *       - Current and past usernames (does broadcast if name was changed)
 *   - UUID<>Username storage/cache
 *     - Database is being built automatically when a player logs in
 *   - Storage for other types of data
 *   - Changes are being autosaved
 * - Can easily be queried via Bukkit API
 *   - Get HalfminerPlayer object to grab stored user data
 *   - Thread safe
 * - Holds customtext caches
 */
@SuppressWarnings({"unused", "SameParameterValue"})
public class HanStorage extends HalfminerClass implements CacheHolder, Disableable, Reloadable {

    private File sysFile;
    private File uuidFile;
    private File playerFile;

    private FileConfiguration sysConfig;
    private FileConfiguration uuidConfig;
    private FileConfiguration playerConfig;

    private final Map<File, CustomtextCache> textCaches = new HashMap<>();

    private BukkitTask task;


    public HanStorage() {
        // dispatch listener class to collect default stats
        new StorageListener(this);
    }

    public HanStorage(Plugin plugin) {
        super(plugin);
    }

    public HanStorage(Plugin plugin, boolean register) {
        super(plugin, register);
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

    public ConfigurationSection getRootSection() {
        return sysConfig;
    }

    public ConfigurationSection getConfigurationSection(String path) {

        if (sysConfig.contains(path) && sysConfig.isConfigurationSection(path)) {
            return sysConfig.getConfigurationSection(path);
        } else {
            return sysConfig.createSection(path);
        }
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
        if (uuidConfig == null) {
            throw new RuntimeException("setUUID(OfflinePlayer) called on non HalfminerSystem HanStorage instance");
        }
        uuidConfig.set(player.getName().toLowerCase(), player.getUniqueId().toString());
    }

    public HalfminerPlayer getPlayer(String playerString) throws PlayerNotFoundException {

        if (uuidConfig == null) {
            throw new RuntimeException("getPlayer(String) called on non HalfminerSystem HanStorage instance");
        }

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
        if (uuidConfig == null) {
            throw new RuntimeException("getPlayer(OfflinePlayer) called on non HalfminerSystem HanStorage instance");
        }
        return new YamlHalfminerPlayer(playerConfig, p);
    }

    public HalfminerPlayer getPlayer(UUID uuid) throws PlayerNotFoundException {
        if (uuidConfig == null) {
            throw new RuntimeException("getPlayer(UUID) called on non HalfminerSystem HanStorage instance");
        }
        return new YamlHalfminerPlayer(playerConfig, uuid);
    }

    public List<HalfminerPlayer> getAllPlayers() {

        if (playerConfig == null) {
            throw new RuntimeException("getAllPlayers() called on non HalfminerSystem HanStorage instance");
        }

        List<HalfminerPlayer> list = new ArrayList<>();
        for (String uuidStr : playerConfig.getKeys(false)) {
            try {
                list.add(getPlayer(UUID.fromString(uuidStr)));
            } catch (PlayerNotFoundException ignored) {}
        }
        return list;
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public CustomtextCache getCache(String fileName) throws CachingException {

        File cacheFile = new File(plugin.getDataFolder(), fileName);

        if (!cacheFile.exists()) {

            // check jar first if it has such file
            if (plugin.getResource(fileName) != null) plugin.saveResource(fileName, false);
            else {
                try {
                    //noinspection ResultOfMethodCallIgnored - we'll check below
                    cacheFile.createNewFile();
                } catch (IOException ignored) {
                }
            }

            if (cacheFile.exists()) {
                MessageBuilder.create("hanStorageCacheCreate")
                        .addPlaceholderReplace("%FILENAME%", cacheFile.getName())
                        .logMessage(Level.INFO);
            } else {
                MessageBuilder.create("hanStorageCacheCouldNotCreate").logMessage(Level.SEVERE);
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
            // only log main plugin saves
            if (plugin == hms) {
                uuidConfig.save(uuidFile);
                playerConfig.save(playerFile);
                MessageBuilder.create("hanStorageSaveSuccessful").logMessage(Level.INFO);
            }
        } catch (Exception e) {
            MessageBuilder.create("hanStorageSaveUnsuccessful").logMessage(Level.WARNING);
            plugin.getLogger().log(Level.WARNING, "Could not save storage for " + plugin.getName(), e);
        }
    }

    @Override
    public void loadConfig() {

        if (sysFile == null) {
            sysFile = new File(plugin.getDataFolder(), "sysdata.yml");
            sysConfig = YamlConfiguration.loadConfiguration(sysFile);
        }

        // UUID and player storage is only ever part of HalfminerSystem,
        // not of plugins who instantiate their own storage instance
        if (uuidFile == null && plugin == hms) {
            uuidFile = new File(hms.getDataFolder(), "uuidcache.yml");
            uuidConfig = YamlConfiguration.loadConfiguration(uuidFile);
        }

        if (playerFile == null && plugin == hms) {
            playerFile = new File(hms.getDataFolder(), "playerdata.yml");
            playerConfig = YamlConfiguration.loadConfiguration(playerFile);
        }

        int saveInterval = hms.getConfig().getInt("handler.storage.autoSaveMinutes", 15) * 60 * 20;
        if (task != null) task.cancel();
        task = scheduler.runTaskTimerAsynchronously(plugin, this::saveConfig, saveInterval, saveInterval);
    }

    @Override
    public void onDisable() {
        saveConfig();
    }
}
