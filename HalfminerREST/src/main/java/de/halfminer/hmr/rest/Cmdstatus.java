package de.halfminer.hmr.rest;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.interfaces.GETCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * - *GET*
 *   - Get current player count
 */
@SuppressWarnings("unused")
public class Cmdstatus extends RESTCommand implements GETCommand {
    @Override
    public NanoHTTPD.Response doOnGET() {
        return ResponseBuilder.getOKResponse(new Status());
    }

    private class Status {
        int playercount = hmw.getServer().getOnlinePlayers().size();
    }
}
