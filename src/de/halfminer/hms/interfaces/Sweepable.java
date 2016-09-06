package de.halfminer.hms.interfaces;

/**
 * Implementing classes have collections that should be sweeped to prevent memory leaks
 */
public interface Sweepable {

    /**
     * Remove no longer necessary data, this method is called on average every 10 minutes
     */
    void sweep();
}
