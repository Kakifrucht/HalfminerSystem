package de.halfminer.hms.manageables;

/**
 * Implementing class must be reloaded on global (re)load
 */
public interface Reloadable extends Manageable {

    /**
     * (Re)loads the modules config
     */
    void loadConfig();
}
