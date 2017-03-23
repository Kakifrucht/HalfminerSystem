package de.halfminer.hms.cache;

import de.halfminer.hms.exceptions.CachingException;
import org.bukkit.plugin.Plugin;

/**
 * Implementing class holds {@link CustomtextCache}'s.
 */
public interface CacheHolder {

    Plugin getPlugin();

    CustomtextCache getCache(String fileName) throws CachingException;
}
