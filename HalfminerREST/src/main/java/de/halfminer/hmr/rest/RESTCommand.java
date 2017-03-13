package de.halfminer.hmr.rest;

import de.halfminer.hmr.HalfminerREST;
import de.halfminer.hmr.gson.GsonUtils;
import de.halfminer.hmr.interfaces.DELETECommand;
import de.halfminer.hmr.interfaces.GETCommand;
import de.halfminer.hmr.interfaces.POSTCommand;
import de.halfminer.hmr.interfaces.PUTCommand;
import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.handlers.HanStorage;
import de.halfminer.hms.util.StringArgumentSeparator;
import fi.iki.elonen.NanoHTTPD;

import java.util.List;
import java.util.Map;

/**
 * Base class for all answers to HTTP REST API requests, in JSON format, producing a {@link NanoHTTPD.Response Response}.
 */
public abstract class RESTCommand extends HalfminerClass {

    final static HalfminerREST hmw = HalfminerREST.getInstance();
    final static HanStorage storage = hms.getStorageHandler();

    StringArgumentSeparator uriParsed;
    Map<String, List<String>> paramsParsed;
    Map<String, String> bodyParsed;

    RESTCommand() {
        super(hmw, false);
    }

    public NanoHTTPD.Response execute(NanoHTTPD.Method method, StringArgumentSeparator uriParsed,
                                      Map<String, List<String>> paramsParsed, Map<String, String> bodyParsed) {
        this.uriParsed = uriParsed;
        this.paramsParsed = paramsParsed;
        this.bodyParsed = bodyParsed;
        if (!doForAll()) returnNotFoundDefault();
        switch (method) {
            case GET:
                if (this instanceof GETCommand) {
                    return ((GETCommand) this).doOnGET();
                } else return returnMethodNotAllowed();
            case POST:
                if (this instanceof POSTCommand) {
                    return ((POSTCommand) this).doOnPOST();
                } else return returnMethodNotAllowed();
            case PUT:
                if (this instanceof PUTCommand) {
                    return ((PUTCommand) this).doOnPUT();
                } else return returnMethodNotAllowed();
            case DELETE:
                if (this instanceof DELETECommand) {
                    return ((DELETECommand) this).doOnDELETE();
                } else return returnMethodNotAllowed();
            default:
                return returnMethodNotAllowed();
        }
    }

    /**
     * Will be called before method specific calls are made.
     *
     * @return true to continue exection, false to stop
     */
    boolean doForAll() {
        return true;
    }

    private NanoHTTPD.Response returnMethodNotAllowed() {
        return returnAnyStatus(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED, GsonUtils.getErrorMap("method not allowed"));
    }

    NanoHTTPD.Response returnOK(Object toSerialize) {
        return returnAnyStatus(NanoHTTPD.Response.Status.OK, toSerialize);
    }

    NanoHTTPD.Response returnNotFound(Object toSerialize) {
        return returnAnyStatus(NanoHTTPD.Response.Status.NOT_FOUND, toSerialize);
    }

    NanoHTTPD.Response returnNotFoundDefault() {
        return returnAnyStatus(NanoHTTPD.Response.Status.NOT_FOUND, GsonUtils.getErrorMap("invalid request"));
    }

    NanoHTTPD.Response returnAnyStatus(NanoHTTPD.Response.Status status, Object toSerialize) {
        return NanoHTTPD.newFixedLengthResponse(status,
                        "application/json", GsonUtils.returnPrettyJson(toSerialize));
    }
}
