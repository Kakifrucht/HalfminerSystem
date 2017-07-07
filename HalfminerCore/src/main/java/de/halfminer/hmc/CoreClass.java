package de.halfminer.hmc;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.handler.HanStorage;

/**
 * Modules and commands of HalfminerCore
 */
@SuppressWarnings("SameParameterValue")
public class CoreClass extends HalfminerClass {

    protected final static HalfminerCore hmc = HalfminerCore.getInstance();
    protected final static HanStorage coreStorage = hmc.getStorage();


    protected CoreClass() {
        super(hmc);
    }

    protected CoreClass(boolean register) {
        super(hmc, register);
    }
}
