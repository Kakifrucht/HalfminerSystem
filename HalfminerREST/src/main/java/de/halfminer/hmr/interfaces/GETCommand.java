package de.halfminer.hmr.interfaces;

import de.halfminer.hmr.rest.RESTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * {@link RESTCommand REST commands's} supporting {@link NanoHTTPD.Method#GET HTTP GET}.
 * Only for data retrieval.
 */
public interface GETCommand {

    NanoHTTPD.Response doOnGET();
}
