package de.halfminer.hmr.interfaces;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.RESTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * {@link RESTCommand REST commands's} supporting {@link NanoHTTPD.Method#PUT HTTP PUT}.
 * Meant to be used for data update/replace or in some cases to create.
 */
public interface PUTCommand {

    ResponseBuilder doOnPUT();
}
