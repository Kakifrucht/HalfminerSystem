package de.halfminer.hms.handlers;

import de.halfminer.hms.HalfminerSystem;

/**
 * HalfminerHandlers are instantiated once and are utility classes that may be used by other modules and commands.
 * They do nothing on their own and work in conjunction with commands and modules.
 */
public abstract class HalfminerHandler {

    final static HalfminerSystem hms = HalfminerSystem.getInstance();

    public void reloadConfig() {
        //does nothing on default, although we don't want submodules who
        //do not use it either having to override an empty function
    }
}
