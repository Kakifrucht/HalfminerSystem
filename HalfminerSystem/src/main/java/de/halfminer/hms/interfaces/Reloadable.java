package de.halfminer.hms.interfaces;

/**
 * Implementing class must be reloaded on global (re)load
 */
public interface Reloadable {

    /**
     * (Re)loads the modules config
     */
    void loadConfig();
}
