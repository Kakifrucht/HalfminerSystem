package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerStorage;
import de.halfminer.hms.HalfminerSystem;

/**
 * HalfminerModules are Singletons instantiated once. They may include Listeners.
 */
public abstract class HalfminerModule {

    final static HalfminerSystem hms = HalfminerSystem.getInstance();
    final static HalfminerStorage storage = hms.getStorage();

    /**
     * Reloads the modules config
     */
    public void reloadConfig() {
        //does nothing on default, although we don't want submodules who
        //do not use it either having to override an empty function
    }

    /**
     * Called when server shuts down
     */
    public void onDisable() {
        //does nothing on default, although we don't want submodules who
        //do not use it either having to write an empty function
    }

}
