package de.halfminer.hmr.rest;

import de.halfminer.hmr.HalfminerREST;
import de.halfminer.hmr.gson.GsonUtils;
import de.halfminer.hmr.interfaces.GETCommand;
import de.halfminer.hmr.interfaces.PUTCommand;
import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.util.StringArgumentSeparator;
import fi.iki.elonen.NanoHTTPD;

import java.util.Map;

/**
 * Base class for all answers to HTTP REST API requests, in JSON format, producing a {@link NanoHTTPD.Response Response}.
 */
public abstract class APICommand extends HalfminerClass {

    final static HalfminerREST hmw = HalfminerREST.getInstance();

    Map<String, String> body;
    StringArgumentSeparator uriParsed;

    APICommand() {
        super(hmw, false);
    }

    public NanoHTTPD.Response execute(NanoHTTPD.Method method,
                                      Map<String, String> body, StringArgumentSeparator parsedRequest) {
        this.body = body;
        this.uriParsed = parsedRequest.removeFirstElement();
        switch (method) {
            case GET:
                if (this instanceof GETCommand) {
                    return ((GETCommand) this).doOnGET();
                } else return returnMethodNotAllowed();
            case POST:
                return returnMethodNotAllowed();
            case PUT:
                if (this instanceof PUTCommand) {
                    return ((PUTCommand) this).doOnPUT();
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
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json",
                GsonUtils.returnPrettyJson(toSerialize));
    }

    NanoHTTPD.Response returnBadRequest(Object toSerialize) {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                "application/json", GsonUtils.returnPrettyJson(toSerialize));
    }

    NanoHTTPD.Response returnBadRequestDefault() {
        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.BAD_REQUEST,
                "application/json", GsonUtils.returnErrorJson("invalid parameters"));
    }
}
