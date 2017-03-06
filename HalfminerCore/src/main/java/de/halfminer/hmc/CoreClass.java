package de.halfminer.hmc;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.handlers.HanStorage;

/**
 * Modules and commands of HalfminerCore
 */
public class CoreClass extends HalfminerClass {

    protected final static HalfminerCore hmc = HalfminerCore.getInstance();
    protected final static HanStorage coreStorage = hmc.getStorage();

    public CoreClass() {
        super(hmc);
    }

    public CoreClass(boolean register) {
        super(hmc, register);
    }
}
