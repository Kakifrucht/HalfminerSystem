package de.halfminer.hms.interfaces;

/**
 * Implementing class must be called before plugin disables in order to shut down properly
 */
public interface Disableable {

    /**
     * Called when plugin gets disabled
     */
    void onDisable();
}
