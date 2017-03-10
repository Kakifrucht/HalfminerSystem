package de.halfminer.hmr.rest.get;

import com.google.gson.Gson;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hmr.rest.APICommand;
import fi.iki.elonen.NanoHTTPD;

import java.util.UUID;

/**
 * Created by fabpw on 09.03.2017.
 */
@SuppressWarnings("unused")
public class Cmduuid extends APICommand {
    @Override
    protected NanoHTTPD.Response execute() {
        if (this.arguments.meetsLength(1)) {
            try {
                HalfminerSystem
                        .getInstance()
                        .getStorageHandler()
                        .getPlayer(arguments.getArgument(0));
            } catch (PlayerNotFoundException e) {
                Response resp = new Response(arguments.getArgument(0), null);
                return returnOKJson(new Gson().toJson(resp));
            }
        }
        return returnInvalidParam();
    }

    private class Response {

        String name;
        UUID uuid;

        Response(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
}
