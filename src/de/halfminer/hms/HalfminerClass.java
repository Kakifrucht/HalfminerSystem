package de.halfminer.hms;

import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Implemented by commands, handlers and modules, to provide shortcuts
 */
public abstract class HalfminerClass {

    protected final static HalfminerSystem hms = HalfminerSystem.getInstance();
    protected final static Server server = hms.getServer();
    protected final static BukkitScheduler scheduler = server.getScheduler();
}
