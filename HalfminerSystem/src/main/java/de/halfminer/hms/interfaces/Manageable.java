package de.halfminer.hms.interfaces;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Interfaces that can be managed by {@link de.halfminer.hms.HalfminerManager}
 */
public interface Manageable {

    /**
     * @return the plugin that owns the class
     */
    JavaPlugin getPlugin();
}
