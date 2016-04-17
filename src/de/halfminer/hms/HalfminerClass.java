package de.halfminer.hms;

import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Implemented by commands, handlers and modules, to provide shortcuts to common objects
 */
public abstract class HalfminerClass {

    protected final static HalfminerSystem hms = HalfminerSystem.getInstance();
    protected final static Server server = hms.getServer();
    protected final static BukkitScheduler scheduler = server.getScheduler();
    protected final static FileConfiguration config = hms.getConfig();
}
