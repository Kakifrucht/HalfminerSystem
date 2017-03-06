package de.halfminer.hmc.modules;

import de.halfminer.hmc.CoreClass;
import de.halfminer.hms.handlers.HanBossBar;
import de.halfminer.hms.handlers.HanHooks;
import de.halfminer.hms.handlers.HanStorage;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.interfaces.Reloadable;

/**
 * HalfminerModules are instantiated once. They may include Listeners.
 * They provide the plugins main functionality.
 */
public abstract class HalfminerModule extends CoreClass implements Reloadable {

    final static HanBossBar barHandler = hms.getBarHandler();
    final static HanHooks hookHandler = hms.getHooksHandler();
    final static HanStorage storage = hms.getStorageHandler();
    final static HanTitles titleHandler = hms.getTitlesHandler();

    @Override
    public void loadConfig() {
        /* does nothing on default, although we don't want to force
           submodules which do not use it either to override it */
    }
}
