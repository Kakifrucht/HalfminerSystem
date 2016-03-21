package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanStorage;
import de.halfminer.hms.interfaces.Reloadable;

/**
 * HalfminerModules are instantiated once. They may include Listeners.
 */
public abstract class HalfminerModule implements Reloadable {

    final static HalfminerSystem hms = HalfminerSystem.getInstance();
    final static HanStorage storage = (HanStorage) hms.getHandler(HandlerType.STORAGE);

    /**
     * Reloads the modules config
     */
    public void reloadConfig() {
        //does nothing on default, although we don't want submodules who
        //do not use it either having to override an empty function
    }
}
