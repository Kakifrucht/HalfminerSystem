package de.halfminer.hmr.rest;

import de.halfminer.hmr.interfaces.POSTCommand;
import de.halfminer.hmr.interfaces.PUTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * Created by fabpw on 11.03.2017.
 */
@SuppressWarnings("unused")
public class Cmdstorage extends RESTCommand implements PUTCommand, POSTCommand {

    @Override
    public NanoHTTPD.Response doOnPUT() {
        return returnBadRequestDefault();
    }

    @Override
    public NanoHTTPD.Response doOnPOST() {
        return doOnPUT();
    }
}
