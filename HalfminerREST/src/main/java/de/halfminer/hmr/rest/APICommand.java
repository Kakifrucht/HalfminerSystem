package de.halfminer.hmr.rest;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hmr.HalfminerREST;
import fi.iki.elonen.NanoHTTPD;

/**
 * Base class for all answers for HTTP API requests, in JSON format.
 */
public abstract class APICommand extends HalfminerClass {

    protected final static HalfminerREST hmw = HalfminerREST.getInstance();

    protected StringArgumentSeparator arguments;

    public APICommand() {
        super(hmw, false);
    }

    public NanoHTTPD.Response execute(StringArgumentSeparator parsedRequest) {
        this.arguments = parsedRequest.removeFirstElement();
        return execute();
    }

    protected abstract NanoHTTPD.Response execute();

    protected NanoHTTPD.Response returnOKJson(String toReturn) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK,
                "application/json", toReturn);
    }

    protected NanoHTTPD.Response returnInvalidParam() {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                "text/plain", "{\"status\": \"invalid parameter\"}");
    }
}
