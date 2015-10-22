package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerStorage;
import de.halfminer.hms.HalfminerSystem;

public abstract class HalfminerModule {

    final static HalfminerSystem hms = HalfminerSystem.getInstance();
    final static HalfminerStorage storage = hms.getStorage();

    public HalfminerModule() {
        reloadConfig();
    }

    /**
     * Reloads the modules config
     */
    public abstract void reloadConfig();

    /**
     * Called when server shuts down
     */
    public void onDisable() {
        //does nothing on default, although we don't want submodules who
        //do not use it either having to write an empty function
    }

}
