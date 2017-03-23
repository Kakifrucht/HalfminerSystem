package de.halfminer.hmr.rest.method;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.cmd.RESTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * {@link RESTCommand REST commands's} supporting {@link NanoHTTPD.Method#POST HTTP POST}.
 * Meant to be used for data creation.
 */
public interface MethodPOST {

    ResponseBuilder doOnPOST();
}
