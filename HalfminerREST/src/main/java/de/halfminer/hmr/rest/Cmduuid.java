package de.halfminer.hmr.rest;

import de.halfminer.hmr.gson.GsonUtils;
import de.halfminer.hmr.interfaces.GETCommand;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.HalfminerPlayer;
import fi.iki.elonen.NanoHTTPD;

import java.util.UUID;

/**
 * - *GET* /< uuid|playername>
 *   - Get a players last known name from UUID or vice versa
 *   - Adds dashes to UUID's, if not supplied
 *   - Returns namechanged boolean, true if supplied username is not current one
 */
@SuppressWarnings("unused")
public class Cmduuid extends RESTCommand implements GETCommand {

    @Override
    public NanoHTTPD.Response doOnGET() {

        if (this.uriParsed.meetsLength(1)) {

            UUID toResolve = null;
            String param = uriParsed.getArgument(0);
            if (param.length() == 36) {
                try {
                    toResolve = UUID.fromString(param);
                } catch (IllegalArgumentException e) {
                    return returnNotFound(GsonUtils.getErrorMap("invalid uuid"));
                }
            } else if (param.length() == 32 && !param.contains("-")) {
                String converted = param.substring(0, 8) + "-"
                        + param.substring(8, 12) + "-"
                        + param.substring(12, 16) + "-"
                        + param.substring(16, 20) + "-"
                        + param.substring(20, 32);
                try {
                    toResolve = UUID.fromString(converted);
                } catch (IllegalArgumentException e) {
                    return returnNotFound(GsonUtils.getErrorMap("invalid uuid"));
                }

            } else if (param.length() > 16) {
                return returnNotFoundDefault();
            }

            try {
                boolean nameChanged;
                HalfminerPlayer hPlayer;
                if (toResolve != null) {
                    hPlayer = hms.getStorageHandler().getPlayer(toResolve);
                    nameChanged = false;
                } else {
                    hPlayer = hms.getStorageHandler().getPlayer(param);
                    nameChanged = !hPlayer.getName().equalsIgnoreCase(param);
                }
                return returnOK(new Response(hPlayer.getName(), hPlayer.getUniqueId(), nameChanged));
            } catch (PlayerNotFoundException e) {
                return returnNotFound(GsonUtils.getErrorMap("unknown player"));
            }
        }

        return returnNotFoundDefault();
    }

    private class Response {

        final String name;
        final String uuid;
        final boolean namechanged;

        Response(String name, UUID uuid, boolean nameChanged) {
            this.name = name;
            this.uuid = uuid.toString();
            this.namechanged = nameChanged;
        }
    }
}
