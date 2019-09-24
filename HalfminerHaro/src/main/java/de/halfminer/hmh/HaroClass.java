package de.halfminer.hmh;

import de.halfminer.hms.HalfminerClass;

public class HaroClass extends HalfminerClass {

    protected final HalfminerHaro hmh;


    protected HaroClass() {
        super(HalfminerHaro.getInstance());
        this.hmh = HalfminerHaro.getInstance();
    }

    protected HaroClass(boolean register) {
        super(HalfminerHaro.getInstance(), register);
        this.hmh = HalfminerHaro.getInstance();
    }
}
