package de.halfminer.hms;

import de.halfminer.hms.interfaces.Manageable;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
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

    protected final JavaPlugin plugin;

    protected HalfminerClass() {
        this.plugin = HalfminerSystem.getInstance();
        halfManager.registerClass(this);
    }

    protected HalfminerClass(JavaPlugin plugin) {
        this.plugin = plugin;
        halfManager.registerClass(this);
    }

    protected HalfminerClass(JavaPlugin plugin, boolean register) {
        this.plugin = plugin;
        if (register) halfManager.registerClass(this);
    }

    @Override
    public JavaPlugin getPlugin() {
        return plugin;
    }
}
