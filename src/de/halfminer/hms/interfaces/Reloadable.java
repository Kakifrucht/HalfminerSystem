package de.halfminer.hms.interfaces;

/**
 * Implementing class can and should be reloaded on config reload
 */
public interface Reloadable {

    /**
     * (Re)loads the modules config
     */
    void loadConfig();
}
