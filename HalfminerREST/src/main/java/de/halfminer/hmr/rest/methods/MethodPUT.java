package de.halfminer.hmr.rest.methods;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.cmd.RESTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * {@link RESTCommand REST commands's} supporting {@link NanoHTTPD.Method#PUT HTTP PUT}.
 * Meant to be used for data update/replace or in some cases to create.
 */
public interface MethodPUT {

    ResponseBuilder doOnPUT();
}
