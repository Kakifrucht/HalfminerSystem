package de.halfminer.hmr.rest.get;

import de.halfminer.hmr.rest.APICommand;
import fi.iki.elonen.NanoHTTPD;

/**
 * - Allows polling minecraft server status
 */
@SuppressWarnings("unused")
public class Cmdstatus extends APICommand {
    @Override
    protected NanoHTTPD.Response execute() {
        
        if (arguments.meetsLength(1)) {
            if (arguments.getArgument(0).equals("get")) {
                return returnOKJson("{\"minecraft\": \"" +
                        hmw.getServer().getOnlinePlayers().size() + "\"}");
            }
        }
        return returnInvalidParam();
    }
}
