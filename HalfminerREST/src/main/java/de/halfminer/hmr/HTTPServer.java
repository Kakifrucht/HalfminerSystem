package de.halfminer.hmr;

import de.halfminer.hmr.gson.GsonUtils;
import de.halfminer.hmr.rest.APICommand;
import de.halfminer.hms.util.StringArgumentSeparator;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Running HTTP server powered by {@link NanoHTTPD}. Calls {@link APICommand} instances after receiving a request.
 * Port and whitelisted IP's must be set via config.
 */
class HTTPServer extends NanoHTTPD {

    private final Set<String> whitelistedIPs;

    HTTPServer(int port, Set<String> whitelistedIPs) throws IOException {
        super(port);
        this.whitelistedIPs = whitelistedIPs;
        this.start();
    }

    @Override
    public NanoHTTPD.Response serve(IHTTPSession session) {

        Map<String, String> headers = session.getHeaders();
        String ipAddress;
        if (headers.containsKey("x-real-ip")) {
            ipAddress = headers.get("x-real-ip");
        } else {
            ipAddress = headers.get("remote-addr");
        }

        if (!whitelistedIPs.contains(ipAddress)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "", "");
        }

        Map<String, String> body = new HashMap<>();
        Method method = session.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(body);
            } catch (Exception e) {
                e.printStackTrace();
                return getBadRequestResponse("invalid arguments");
            }
        }

        APICommand command;
        StringArgumentSeparator parsedRequest = new StringArgumentSeparator(session.getUri().substring(1), '/');
        try {
            command = (APICommand) this.getClass()
                    .getClassLoader()
                    .loadClass("de.halfminer.hmr.rest.Cmd" + parsedRequest.getArgument(0).toLowerCase())
                    .newInstance();
        } catch (ClassNotFoundException e) {
            return getBadRequestResponse("unsupported");
        } catch (Exception e) {
            e.printStackTrace();
            return getInternalErrorResponse();
        }

        try {
            return command.execute(method, body, parsedRequest);
        } catch (Throwable e) {
            e.printStackTrace();
            return getInternalErrorResponse();
        }
    }

    private NanoHTTPD.Response getBadRequestResponse(String message) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                "application/json", GsonUtils.returnErrorJson(message));
    }

    private NanoHTTPD.Response getInternalErrorResponse() {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                "text/plain", "An internal error has occurred");
    }
}
