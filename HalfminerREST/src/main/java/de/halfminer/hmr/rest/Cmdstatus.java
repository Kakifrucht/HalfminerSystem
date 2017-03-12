package de.halfminer.hmr.rest;

import de.halfminer.hmr.interfaces.GETCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * - Get current player count
 */
@SuppressWarnings("unused")
public class Cmdstatus extends RESTCommand implements GETCommand {
    @Override
    public NanoHTTPD.Response doOnGET() {
        return returnOK(new Status());
    }

    private class Status {
        int playercount = hmw.getServer().getOnlinePlayers().size();
    }
}
