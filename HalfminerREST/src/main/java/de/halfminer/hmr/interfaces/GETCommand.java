package de.halfminer.hmr.interfaces;

import fi.iki.elonen.NanoHTTPD;

/**
 * {@link de.halfminer.hmr.rest.APICommand REST commands's} supporting {@link NanoHTTPD.Method#GET HTTP GET}.
 * Only for data retrieval.
 */
public interface GETCommand {

    NanoHTTPD.Response doOnGET();
}
