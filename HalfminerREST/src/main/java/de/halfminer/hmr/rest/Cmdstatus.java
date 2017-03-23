package de.halfminer.hmr.rest;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.method.GETCommand;

/**
 * - *GET*
 *   - Get current player count
 */
@SuppressWarnings("unused")
public class Cmdstatus extends RESTCommand implements GETCommand {
    @Override
    public ResponseBuilder doOnGET() {
        return ResponseBuilder.getOKResponse(new Status());
    }

    private class Status {
        int playercount = hmw.getServer().getOnlinePlayers().size();
    }
}
