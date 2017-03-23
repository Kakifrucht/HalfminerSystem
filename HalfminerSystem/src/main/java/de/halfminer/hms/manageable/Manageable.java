package de.halfminer.hms.manageable;

import org.bukkit.plugin.Plugin;

/**
 * Interfaces that can be managed by {@link HalfminerManager}
 */
public interface Manageable {

    /**
     * @return the plugin that owns the class
     */
    Plugin getPlugin();
}
