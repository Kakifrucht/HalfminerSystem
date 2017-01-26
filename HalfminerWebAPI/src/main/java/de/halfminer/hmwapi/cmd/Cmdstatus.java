package de.halfminer.hmwapi.cmd;

import fi.iki.elonen.NanoHTTPD;

/**
 * - Allows polling minecraft server status (get)
 */
@SuppressWarnings("unused")
public class Cmdstatus extends APICommand {
    @Override
    public NanoHTTPD.Response execute() {

        session.getParms();
        if (arguments.meetsLength(1)) {
            if (arguments.getArgument(0).equals("get")) {
                return returnOKJson("{\"minecraft\": \"" +
                        hmw.getServer().getOnlinePlayers().size() + "\"}");
            } /*else if (arguments.getArgument(0).equals("set")) {
                //TODO allow external clients to add their own status
            } */
        }
        return returnInvalidParam();
    }
}
