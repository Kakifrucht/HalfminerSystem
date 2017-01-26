package de.halfminer.hmwapi.cmd;

import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hmwapi.HalfminerWebAPI;
import fi.iki.elonen.NanoHTTPD;

/**
 * Base class for all answers for HTTP API requests, in JSON format.
 */
public abstract class APICommand {

    final HalfminerWebAPI hmw = HalfminerWebAPI.getInstance();

    NanoHTTPD.IHTTPSession session;
    StringArgumentSeparator arguments;

    public NanoHTTPD.Response execute(NanoHTTPD.IHTTPSession session, StringArgumentSeparator parsedRequest) {
        this.session = session;
        this.arguments = parsedRequest.removeFirstElement();
        return execute();
    }

    public abstract NanoHTTPD.Response execute();

    NanoHTTPD.Response returnOKJson(String toReturn) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                "application/json", toReturn);
    }

    NanoHTTPD.Response returnInvalidParam() {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                "text/plain", "{\"status\": \"invalid parameter\"}");
    }
}
