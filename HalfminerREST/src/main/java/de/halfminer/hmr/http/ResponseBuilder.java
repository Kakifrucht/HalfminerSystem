package de.halfminer.hmr.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fi.iki.elonen.NanoHTTPD;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds {@link NanoHTTPD.Response HTTP Responses} to be dispatched by {@link HTTPServer}. Provides shortcuts
 * to often used responses and allows setting an Object to serialize/String to send, defining the response code,
 * setting the MIME type and allowing single key:value JSON responses for errors and messages.
 */
@SuppressWarnings("ALL")
public class ResponseBuilder {

    public static ResponseBuilder getAnyResponse(NanoHTTPD.Response.Status status, Object toSerialize) {
        return create().setStatus(status).setObjectToSerialize(toSerialize);
    }

    public static ResponseBuilder getOKResponse(Object toSerialize) {
        return getAnyResponse(NanoHTTPD.Response.Status.OK, toSerialize);
    }

    public static ResponseBuilder getNotFoundResponse(String errorMessage) {
        return create().setStatus(NanoHTTPD.Response.Status.NOT_FOUND).setError(errorMessage);
    }

    public static ResponseBuilder create() {
        return new ResponseBuilder();
    }

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private NanoHTTPD.Response.Status status = NanoHTTPD.Response.Status.OK;
    private Object toSerialize = "";
    private String mimeType = "application/json";

    private Map<String, String> additionalHeaders;

    private ResponseBuilder() {}

    public ResponseBuilder setStatus(NanoHTTPD.Response.Status status) {
        this.status = status;
        return this;
    }

    public ResponseBuilder setObjectToSerialize(Object toSerialize) {
        this.toSerialize = toSerialize;
        return this;
    }

    public ResponseBuilder setSingleton(String key, String value) {
        this.toSerialize = Collections.singletonMap(key, value);
        return this;
    }

    public ResponseBuilder setError(String message) {
        return setSingleton("error", message);
    }

    public ResponseBuilder setMimeType(String mimeType) {
        this.mimeType = mimeType;
        return this;
    }

    public ResponseBuilder addHeader(String name, String value) {
        if (additionalHeaders == null) additionalHeaders = new HashMap<>();
        additionalHeaders.put(name, value);
        return this;
    }

    public NanoHTTPD.Response returnResponse() {
        String toSend = toSerialize instanceof String ? (String) toSerialize : gson.toJson(toSerialize);
        NanoHTTPD.Response response = NanoHTTPD.newFixedLengthResponse(status, mimeType, toSend);
        if (additionalHeaders != null) {
            for (String name : additionalHeaders.keySet()) {
                response.addHeader(name, additionalHeaders.get(name));
            }
        }
        return response;
    }
}
