package de.halfminer.hml;

import de.halfminer.hml.land.contract.ContractManager;
import de.halfminer.hms.HalfminerClass;

public class LandClass extends HalfminerClass {

    protected final HalfminerLand hml;
    protected final ContractManager contractManager;


    protected LandClass() {
        super(HalfminerLand.getInstance());
        this.hml = HalfminerLand.getInstance();
        this.contractManager = hml.getContractManager();
    }

    protected LandClass(boolean register) {
        super(HalfminerLand.getInstance(), register);
        this.hml = HalfminerLand.getInstance();
        this.contractManager = hml.getContractManager();
    }
}
