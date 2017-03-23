package de.halfminer.hmr.method;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.RESTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * {@link RESTCommand REST commands's} supporting {@link NanoHTTPD.Method#GET HTTP GET}.
 * Meant to be used for data retrieval.
 */
public interface GETCommand {

    ResponseBuilder doOnGET();
}
