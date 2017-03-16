package de.halfminer.hmr.interfaces;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.RESTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * {@link RESTCommand REST commands's} supporting {@link NanoHTTPD.Method#DELETE HTTP DELETE}.
 * Meant to be used for data deletion.
 */
public interface DELETECommand {

    ResponseBuilder doOnDELETE();
}
