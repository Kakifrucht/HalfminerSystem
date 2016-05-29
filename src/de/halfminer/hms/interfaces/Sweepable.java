package de.halfminer.hms.interfaces;

/**
 * Classes implementing this have maps/lists that can and should be sweeped to prevent memory leaks
 */
public interface Sweepable {

    /**
     * Remove no longer necessary data, this method is called on average every 10 minutes
     */
    void sweep();
}
