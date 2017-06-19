package de.halfminer.hmr.http;

import de.halfminer.hmr.rest.cmd.RESTCommand;
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

    private final Logger logger;
    private final Set<String> whitelistedIPs;
    private final boolean proxyMode;

    public HTTPServer(Logger logger, int port, Set<String> whitelistedIPs, boolean proxyMode) throws IOException {
        super(port);
        this.logger = logger;
        this.whitelistedIPs = whitelistedIPs;
        this.proxyMode = proxyMode;
        this.start();
    }

    @Override
    public NanoHTTPD.Response serve(IHTTPSession session) {

        Map<String, String> headers = session.getHeaders();
        String ipAddress;
        if (proxyMode && headers.containsKey("x-real-ip")) {
            ipAddress = headers.get("x-real-ip");
        } else {
            ipAddress = session.getRemoteIpAddress();
        }

        if (!whitelistedIPs.contains(ipAddress)) {
            return ResponseBuilder.create().setStatus(Response.Status.FORBIDDEN).setMimeType("").returnResponse();
        }

        Map<String, String> bodyParsed = null;
        Method method = session.getMethod();

        // read body for POST/PUT/DELETE
        if (!Method.GET.equals(method)) {

            // only parse application/x-www-form-urlencoded, disallow different body types for the time being
            if (!headers.containsKey("content-type")
                    || !headers.get("content-type").startsWith("application/x-www-form-urlencoded")) {
                return ResponseBuilder
                        .getNotFoundResponse("content-type must be application/x-www-form-urlencoded")
                        .returnResponse();
            }

            bodyParsed = new HashMap<>();

            int contentLength;
            try {
                contentLength = Integer.parseInt(headers.get("content-length"));
            } catch (NumberFormatException e) {
                return ResponseBuilder.getNotFoundResponse("invalid header").returnResponse();
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
                    .loadClass("de.halfminer.hmr.rest.cmd.Cmd" + parsedRequest.getArgument(0).toLowerCase())
                    .newInstance();
        } catch (ClassNotFoundException e) {
            return ResponseBuilder.getNotFoundResponse("unsupported").returnResponse();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Internal error during command instantiation", e);
            return getInternalErrorResponse();
        }

        try {
            return command
                    .execute(method,
                            "http://" + headers.get("host") + session.getUri(), // determine if https behind proxy?
                            parsedRequest.removeFirstElement(),
                            session.getParameters(), bodyParsed)
                    .returnResponse();
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Catch-all caught exception during command execution", e);
            return getInternalErrorResponse();
        }
    }

    private NanoHTTPD.Response getInternalErrorResponse() {
        return ResponseBuilder.create()
                .setMimeType(MIME_PLAINTEXT)
                .setObjectToSerialize("An internal error has occurred")
                .returnResponse();
    }

    public boolean isProxyMode() {
        return proxyMode;
    }
}
