package de.halfminer.hms.interfaces;

import de.halfminer.hms.caches.CustomtextCache;
import de.halfminer.hms.exception.CachingException;
import org.bukkit.plugin.Plugin;

/**
 * Implementing class holds {@link CustomtextCache}'s.
 */
public interface CacheHolder {

    Plugin getPlugin();

    CustomtextCache getCache(String fileName) throws CachingException;
}
