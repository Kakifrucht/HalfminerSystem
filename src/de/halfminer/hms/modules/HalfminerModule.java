package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.*;
import de.halfminer.hms.interfaces.Reloadable;

/**
 * HalfminerModules are instantiated once. They may include Listeners.
 * They provide the plugins main functionality.
 */
public abstract class HalfminerModule extends HalfminerClass implements Reloadable {

    final static HanBossBar barHandler = (HanBossBar) hms.getHandler(HandlerType.BOSS_BAR);
    final static HanHooks hookHandler = (HanHooks) hms.getHandler(HandlerType.HOOKS);
    final static HanStorage storage = (HanStorage) hms.getHandler(HandlerType.STORAGE);
    final static HanTeleport teleportHandler = (HanTeleport) hms.getHandler(HandlerType.TELEPORT);
    final static HanTitles titleHandler = (HanTitles) hms.getHandler(HandlerType.TITLES);

    @Override
    public void loadConfig() {
        /* does nothing on default, although we don't want to force
           submodules which do not use it either to override it */
    }
}
