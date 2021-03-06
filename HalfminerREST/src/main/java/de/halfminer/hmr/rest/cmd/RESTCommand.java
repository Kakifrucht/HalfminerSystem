package de.halfminer.hmr.rest.cmd;

import de.halfminer.hmr.HalfminerREST;
import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.methods.MethodDELETE;
import de.halfminer.hmr.rest.methods.MethodGET;
import de.halfminer.hmr.rest.methods.MethodPOST;
import de.halfminer.hmr.rest.methods.MethodPUT;
import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.handler.HanStorage;
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

    String url;
    StringArgumentSeparator uriParsed;
    Map<String, List<String>> paramsParsed;
    Map<String, String> bodyParsed;

    RESTCommand() {
        super(hmw, false);
    }

    public ResponseBuilder execute(NanoHTTPD.Method method, String url, StringArgumentSeparator uriParsed,
                                      Map<String, List<String>> paramsParsed, Map<String, String> bodyParsed) {
        this.url = url;
        this.uriParsed = uriParsed;
        this.paramsParsed = paramsParsed;
        this.bodyParsed = bodyParsed;
        if (!doForAll()) returnNotFoundDefault();
        switch (method) {
            case GET:
                if (this instanceof MethodGET) {
                    return ((MethodGET) this).doOnGET();
                } else return returnMethodNotAllowed();
            case POST:
                if (this instanceof MethodPOST) {
                    return ((MethodPOST) this).doOnPOST();
                } else return returnMethodNotAllowed();
            case PUT:
                if (this instanceof MethodPUT) {
                    return ((MethodPUT) this).doOnPUT();
                } else return returnMethodNotAllowed();
            case DELETE:
                if (this instanceof MethodDELETE) {
                    return ((MethodDELETE) this).doOnDELETE();
                } else return returnMethodNotAllowed();
            default:
                return returnMethodNotAllowed();
        }
    }

    /**
     * Will be called before method specific calls are made.
     *
     * @return true to continue method specific execution of command, false to stop
     */
    boolean doForAll() {
        return true;
    }

    private ResponseBuilder returnMethodNotAllowed() {
        return ResponseBuilder.create()
                .setStatus(NanoHTTPD.Response.Status.METHOD_NOT_ALLOWED)
                .setError("method not allowed");
    }

    ResponseBuilder returnNotFoundDefault() {
        return ResponseBuilder.getNotFoundResponse("invalid request");
    }
}
