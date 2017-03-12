package de.halfminer.hmr.interfaces;

import de.halfminer.hmr.rest.RESTCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * {@link RESTCommand REST commands's} supporting {@link NanoHTTPD.Method#PUT HTTP PUT}.
 * Meant for data creation and updating.
 */
public interface PUTCommand {

    NanoHTTPD.Response doOnPUT();
}
