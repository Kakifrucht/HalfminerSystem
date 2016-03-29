package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanStorage;
import de.halfminer.hms.interfaces.Reloadable;

/**
 * HalfminerModules are instantiated once. They may include Listeners.
 */
public abstract class HalfminerModule extends HalfminerClass implements Reloadable {

    final static HanStorage storage = (HanStorage) hms.getHandler(HandlerType.STORAGE);

    @Override
    public void loadConfig() {
        /* does nothing on default, although we don't want submodules who
           do not use it either having to override an empty function */
    }
}
