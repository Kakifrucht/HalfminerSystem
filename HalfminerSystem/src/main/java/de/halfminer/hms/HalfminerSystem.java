package de.halfminer.hms;

import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * HalfminerSystem main class, base API for Halfminer Bukkit/Spigot plugins
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerSystem extends JavaPlugin {

    private static HalfminerSystem instance;

    public static HalfminerSystem getInstance() {
        return instance;
    }

    private HalfminerManager manager;
    private final Map<HandlerType, HalfminerClass> handlers = new HashMap<>();

    @Override
    public void onEnable() {

        instance = this;
        manager = new HalfminerManager(this);
        manager.reloadOcurred(this);

        try {
            // Load handlers
            for (HandlerType handler : HandlerType.values()) {
                handlers.put(handler, (HalfminerClass) this.getClassLoader()
                        .loadClass("de.halfminer.hms.handlers." + handler.getClassName())
                        .newInstance());
            }
        } catch (Exception e) {
            getLogger().severe("An error has occurred, see stacktrace for information");
            e.printStackTrace();
            setEnabled(false);
            return;
        }
        getLogger().info("HalfminerSystem enabled");
    }

    public HalfminerManager getHalfminerManager() {
        return manager;
    }

    public HanBossBar getBarHandler() {
        return (HanBossBar) getHandler(HandlerType.BOSS_BAR);
    }

    public HanHooks getHooksHandler() {
        return (HanHooks) getHandler(HandlerType.HOOKS);
    }

    public HanStorage getStorageHandler() {
        return (HanStorage) getHandler(HandlerType.STORAGE);
    }

    public HanTeleport getTeleportHandler() {
        return (HanTeleport) getHandler(HandlerType.TELEPORT);
    }

    public HanTitles getTitlesHandler() {
        return (HanTitles) getHandler(HandlerType.TITLES);
    }

    private HalfminerClass getHandler(HandlerType type) {
        if (handlers.size() != HandlerType.values().length)
            throw new RuntimeException("Illegal call to getHandler before all handlers were initialized");
        return handlers.get(type);
    }
}
