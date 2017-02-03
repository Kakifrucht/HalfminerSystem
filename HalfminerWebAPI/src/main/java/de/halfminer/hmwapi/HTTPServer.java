package de.halfminer.hmwapi;

import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hmwapi.cmd.APICommand;
import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.Set;

/**
 * Running HTTP server powered by {@link NanoHTTPD}. Calls {@link APICommand} instances after receiving a request.
 * Port and whitelisted IP's must be set via config.
 */
public class HTTPServer extends NanoHTTPD {

    private final Set<String> whitelistedIPs;

    HTTPServer(int port, Set<String> whitelistedIPs) throws IOException {
        super(port);
        this.whitelistedIPs = whitelistedIPs;
        this.start();
    }

    @Override
    public NanoHTTPD.Response serve(IHTTPSession session) {

        String ipAddress = session.getHeaders().get("remote-addr");
        if (!whitelistedIPs.contains(ipAddress)) {
            return newFixedLengthResponse(Response.Status.FORBIDDEN, "", "");
        }

        StringArgumentSeparator parsedRequest = new StringArgumentSeparator(session.getUri().substring(1), '/');
        APICommand command;
        try {
            command = (APICommand) this.getClass()
                    .getClassLoader()
                    .loadClass("de.halfminer.hmwapi.cmd.Cmd" + parsedRequest.getArgument(0).toLowerCase())
                    .newInstance();
        } catch (ClassNotFoundException e) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "", "");
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                    "text/plain", "An internal error has occurred");
        }

        return command.execute(session, parsedRequest);
    }
}
