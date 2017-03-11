package de.halfminer.hmr.interfaces;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by fabpw on 11.03.2017.
 */
public interface PUTCommand {

    NanoHTTPD.Response doOnPUT();
}
