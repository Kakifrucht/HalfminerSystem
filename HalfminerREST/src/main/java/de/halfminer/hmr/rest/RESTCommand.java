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

import java.util.Map;

/**
 * Base class for all answers to HTTP REST API requests, in JSON format, producing a {@link NanoHTTPD.Response Response}.
 */
public abstract class RESTCommand extends HalfminerClass {

    final static HalfminerREST hmw = HalfminerREST.getInstance();
    final static HanStorage storage = hms.getStorageHandler();

    Map<String, String> bodyParsed;
    StringArgumentSeparator uriParsed;

    RESTCommand() {
        super(hmw, false);
    }

    public NanoHTTPD.Response execute(NanoHTTPD.Method method,
                                      Map<String, String> body, StringArgumentSeparator parsedRequest) {
        this.bodyParsed = body;
        this.uriParsed = parsedRequest.removeFirstElement();
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

    private NanoHTTPD.Response returnMethodNotAllowed() {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED,
                "application/json", GsonUtils.returnErrorJson("method not allowed"));
    }

    NanoHTTPD.Response returnOK(Object toSerialize) {
        return returnAnyStatus(NanoHTTPD.Response.Status.OK, toSerialize);
    }

    NanoHTTPD.Response returnNotFound(Object toSerialize) {
        return returnAnyStatus(NanoHTTPD.Response.Status.NOT_FOUND, toSerialize);
    }

    NanoHTTPD.Response returnNotFoundDefault() {
        return returnAnyStatus(NanoHTTPD.Response.Status.NOT_FOUND,
                GsonUtils.returnErrorJson("invalid parameters"));
    }

    NanoHTTPD.Response returnAnyStatus(NanoHTTPD.Response.Status status, Object toSerialize) {
        return NanoHTTPD
                .newFixedLengthResponse(status, "application/json", GsonUtils.returnPrettyJson(toSerialize));
    }
}
