package de.halfminer.hms.interfaces;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.caches.CustomtextCache;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Implementing class holds {@link CustomtextCache}'s.
 */
public interface CacheHolder {

    JavaPlugin getPlugin();

    CustomtextCache getCache(String fileName) throws CachingException;
}
