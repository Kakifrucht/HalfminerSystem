package de.halfminer.hms.interfaces;

import org.bukkit.plugin.Plugin;

/**
 * Interfaces that can be managed by {@link de.halfminer.hms.HalfminerManager}
 */
public interface Manageable {

    /**
     * @return the plugin that owns the class
     */
    Plugin getPlugin();
}
