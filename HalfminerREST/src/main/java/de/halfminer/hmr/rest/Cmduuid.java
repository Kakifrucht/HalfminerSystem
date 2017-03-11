package de.halfminer.hmr.rest;

import de.halfminer.hmr.interfaces.GETCommand;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.HalfminerPlayer;
import fi.iki.elonen.NanoHTTPD;

import java.util.UUID;

/**
 * - Get a players last known name from UUID or vice versa
 * - Adds dashes to UUID's, if not supplied
 */
@SuppressWarnings("unused")
public class Cmduuid extends APICommand implements GETCommand {

    @Override
    public NanoHTTPD.Response doOnGET() {

        if (this.uriParsed.meetsLength(1)) {

            UUID toResolve = null;
            String param = uriParsed.getArgument(0);
            if (param.length() == 36) {
                try {
                    toResolve = UUID.fromString(param);
                } catch (IllegalArgumentException e) {
                    return returnBadRequest(new Response());
                }
            } else if (param.length() == 32 && !param.contains("-")) {
                String converted = param.substring(0, 8) + "-"
                        + param.substring(8, 12) + "-"
                        + param.substring(12, 16) + "-"
                        + param.substring(16, 20) + "-"
                        + param.substring(20, 32);
                toResolve = UUID.fromString(converted);
            } else if (param.length() > 16) {
                return returnBadRequestDefault();
            }

            try {
                HalfminerPlayer hPlayer;
                if (toResolve != null) {
                    hPlayer = hms.getStorageHandler().getPlayer(toResolve);
                } else {
                    hPlayer = hms.getStorageHandler().getPlayer(param);
                }
                return returnOK(new Response(hPlayer.getName(), hPlayer.getUniqueId()));
            } catch (PlayerNotFoundException e) {
                return returnBadRequest(new Response(uriParsed.getArgument(0)));
            }
        }

        return returnBadRequestDefault();
    }

    private class Response {

        String name = null;
        String uuid = null;
        String error = null;

        Response() {
            error = "UUID not found";
        }

        Response(String name) {
            this.name = name;
            this.error = "unknown player";
        }

        Response(String name, UUID uuid) {
            if (name.isEmpty() || uuid == null) {
                error = "unknown player";
            } else {
                this.name = name;
                this.uuid = uuid.toString();
            }
        }
    }
}
