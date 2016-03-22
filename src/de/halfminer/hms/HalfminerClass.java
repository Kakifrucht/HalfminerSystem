package de.halfminer.hms;

import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * Implemented by commands, handlers and modules, to provide shortcuts
 */
public abstract class HalfminerClass {

    public final static HalfminerSystem hms = HalfminerSystem.getInstance();
    public final static Server server = hms.getServer();
    public final static BukkitScheduler scheduler = server.getScheduler();
}
