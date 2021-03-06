package de.halfminer.hms.manageable;

/**
 * Implementing class must be called before plugin disables in order to shut down properly
 */
public interface Disableable extends Manageable {

    /**
     * Called when plugin gets disabled
     */
    void onDisable();
}
