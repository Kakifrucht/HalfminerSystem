package de.halfminer.hmr.rest;

import de.halfminer.hmr.interfaces.GETCommand;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.HalfminerPlayer;
import fi.iki.elonen.NanoHTTPD;

import java.util.UUID;

/**
 * - Get a players last known name from UUID or vice versa
 */
@SuppressWarnings("unused")
public class Cmduuid extends APICommand implements GETCommand {

    @Override
    public NanoHTTPD.Response doOnGET() {

        if (this.arguments.meetsLength(1)) {

            String param = arguments.getArgument(0);
            try {
                HalfminerPlayer hPlayer;
                if (param.length() > 16) {
                    hPlayer = hms.getStorageHandler().getPlayer(UUID.fromString(param));
                } else {
                    hPlayer = hms.getStorageHandler().getPlayer(param);
                }
                return returnOK(new Response(hPlayer.getName(), hPlayer.getUniqueId()));
            } catch (PlayerNotFoundException e) {
                return returnOK(new Response(arguments.getArgument(0)));
            } catch (IllegalArgumentException e) {
                return returnOK(new Response());
            }
        }

        return returnInvalidParam();
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
