package de.halfminer.hmr.rest;

import de.halfminer.hmr.interfaces.GETCommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * - Allows polling minecraft server status
 */
@SuppressWarnings("unused")
public class Cmdstatus extends APICommand implements GETCommand {
    @Override
    public NanoHTTPD.Response doOnGET() {
        
        if (arguments.meetsLength(1)) {
            if (arguments.getArgument(0).equals("playercount")) {
                return returnOK("{\"minecraft\": \"" +
                        hmw.getServer().getOnlinePlayers().size() + "\"}");
            }
        }
        return returnInvalidParam();
    }
}
