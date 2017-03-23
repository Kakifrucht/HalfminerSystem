package de.halfminer.hms;

import de.halfminer.hms.manageables.HalfminerManager;
import de.halfminer.hms.manageables.Manageable;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Implementing class will have access to common interfaces and will be registered in {@link HalfminerManager}
 * for automatic sweeping, reloading and calling of functions before the plugin disables
 */
public abstract class HalfminerClass implements Manageable {

    protected final static HalfminerSystem hms = HalfminerSystem.getInstance();
    private final static HalfminerManager halfManager = hms.getHalfminerManager();
    protected final static Server server = hms.getServer();
    protected final static BukkitScheduler scheduler = server.getScheduler();

    protected final Plugin plugin;

    protected HalfminerClass() {
        this.plugin = HalfminerSystem.getInstance();
        registerClass();
    }

    protected HalfminerClass(Plugin plugin) {
        this.plugin = plugin;
        registerClass();
    }

    protected HalfminerClass(Plugin plugin, boolean register) {
        this.plugin = plugin;
        if (register) registerClass();
    }

    protected void registerClass() {
        halfManager.registerClass(this);
    }

    protected void unregisterClass() {
        halfManager.unregisterClass(this);
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }
}
