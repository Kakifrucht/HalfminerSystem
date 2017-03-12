package de.halfminer.hmr;

import de.halfminer.hmr.gson.GsonUtils;
import de.halfminer.hmr.rest.RESTCommand;
import de.halfminer.hms.util.StringArgumentSeparator;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Running HTTP server powered by {@link NanoHTTPD}. Calls {@link RESTCommand} instances after receiving a request.
 * Port and whitelisted IP's must be set via config.
 */
public class HTTPServer extends NanoHTTPD {

    public static String lastHOST;

    private final Logger logger;
    private final Set<String> whitelistedIPs;

    HTTPServer(Logger logger, int port, Set<String> whitelistedIPs) throws IOException {
        super(port);
        this.logger = logger;
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

        Map<String, String> bodyParsed = null;
        Method method = session.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {

            bodyParsed = new HashMap<>();

            int contentLength;
            try {
                contentLength = Integer.parseInt(headers.get("content-length"));
            } catch (NumberFormatException e) {
                return getBadRequestResponse("invalid header");
            }

            byte[] buffer = new byte[contentLength];
            try {
                //noinspection ResultOfMethodCallIgnored
                session.getInputStream().read(buffer, 0, contentLength);
            } catch (IOException e) {
                logger.log(Level.WARNING, "InputStream could not be read", e);
                return getInternalErrorResponse();
            }

            // parse content body
            int lastSubstring = 0;
            String currentKey = "";
            String toParse = new String(buffer);
            for (int i = 0; i < toParse.length(); i++) {
                if (toParse.charAt(i) == '=' || toParse.charAt(i) == '&') {
                    if (currentKey.length() > 0) {
                        bodyParsed.put(currentKey, toParse.substring(lastSubstring, i));
                        currentKey = "";
                    } else {
                        currentKey = toParse.substring(lastSubstring, i);
                    }
                    lastSubstring = i + 1;
                }
            }

            if (currentKey.length() > 0) {
                bodyParsed.put(currentKey, toParse.substring(lastSubstring, toParse.length()));
            }
        }

        RESTCommand command;
        StringArgumentSeparator parsedRequest = new StringArgumentSeparator(session.getUri().substring(1), '/');
        try {
            command = (RESTCommand) this.getClass()
                    .getClassLoader()
                    .loadClass("de.halfminer.hmr.rest.Cmd" + parsedRequest.getArgument(0).toLowerCase())
                    .newInstance();
        } catch (ClassNotFoundException e) {
            return getBadRequestResponse("unsupported");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Internal error during command instantiation", e);
            return getInternalErrorResponse();
        }

        lastHOST = headers.get("HOST") + session.getUri();
        try {
            return command.execute(method, bodyParsed, parsedRequest);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Catch all exception during command execution", e);
            return getInternalErrorResponse();
        }
    }

    private NanoHTTPD.Response getBadRequestResponse(String message) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST,
                "application/json", GsonUtils.returnErrorJson(message));
    }

    private NanoHTTPD.Response getInternalErrorResponse() {
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT, "An internal error has occurred");
    }
}
