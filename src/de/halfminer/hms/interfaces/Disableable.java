package de.halfminer.hms.interfaces;

/**
 * Class must be called before plugin disable in order to shut down properly
 */
public interface Disableable {

    /**
     * Called when plugin gets disabled
     */
    void onDisable();
}
