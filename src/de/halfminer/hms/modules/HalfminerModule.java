package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;

public abstract class HalfminerModule {

    protected final static HalfminerSystem hms = HalfminerSystem.getInstance();

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
