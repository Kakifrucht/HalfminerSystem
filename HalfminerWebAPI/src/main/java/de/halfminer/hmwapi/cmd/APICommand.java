package de.halfminer.hmwapi.cmd;

import com.google.gson.Gson;
import de.halfminer.hms.util.StringArgumentSeparator;
import fi.iki.elonen.NanoHTTPD;

/**
 * Base class for all answers for HTTP API requests, in JSON format.
 */
public abstract class APICommand {

    public abstract Gson execute(NanoHTTPD.IHTTPSession session, StringArgumentSeparator parsedRequest);
}
