package de.halfminer.hms.interfaces;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.util.CustomtextCache;

/**
 * Implementing class holds {@link CustomtextCache}'s.
 */
public interface CacheHolder {

    CustomtextCache getCache(String fileName) throws CachingException;
}
