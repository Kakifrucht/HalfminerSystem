package de.halfminer.hmr.rest.cmd;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.methods.MethodGET;

/**
 * - *GET*
 *   - Get current player count
 */
@SuppressWarnings("unused")
public class Cmdstatus extends RESTCommand implements MethodGET {
    @Override
    public ResponseBuilder doOnGET() {
        return ResponseBuilder.getOKResponse(new Status());
    }

    private static class Status {
        int playercount = hmw.getServer().getOnlinePlayers().size();
    }
}
