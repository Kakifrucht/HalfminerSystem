package de.halfminer.hmr.rest.cmd;

import de.halfminer.hmr.http.ResponseBuilder;
import de.halfminer.hmr.rest.methods.MethodGET;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * - *GET* /< uuid|playername>[/stats|/nodashes]
 *   - Get a players last known name from UUID or vice versa
 *   - Adds dashes to UUID's, if not supplied
 *   - Returns namechanged boolean, true if supplied username is not current one
 *   - If /stats argument supplied via URI, will append all recorded stats about player
 *   - If /nodashes argument supplied, will remove dashes from returned UUID
 */
@SuppressWarnings("unused")
public class Cmdplayer extends RESTCommand implements MethodGET {

    @Override
    public ResponseBuilder doOnGET() {

        if (this.uriParsed.meetsLength(1)) {

            UUID toResolve = null;
            String param = uriParsed.getArgument(0);
            if (param.length() == 36) {
                try {
                    toResolve = UUID.fromString(param);
                } catch (IllegalArgumentException e) {
                    return ResponseBuilder.getNotFoundResponse("invalid uuid");
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
                    return ResponseBuilder.getNotFoundResponse("invalid uuid");
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

                String uuid = hPlayer.getUniqueId().toString();
                Map<String, String> stats = null;
                if (uriParsed.meetsLength(2)) {

                    String argument = uriParsed.getArgument(1).toLowerCase();
                    if (argument.equals("stats")) {
                        stats = new HashMap<>();
                        for (DataType dataType : DataType.values()) {
                            String value = hPlayer.getString(dataType);
                            if (value.length() > 0) {
                                stats.put(dataType.toString(), value);
                            }
                        }
                    } else if (argument.equals("nodashes")) {
                        uuid = uuid.replaceAll("-", "");
                    }
                }

                return ResponseBuilder
                        .getOKResponse(new Response(hPlayer.getName(), uuid, nameChanged, stats));

            } catch (PlayerNotFoundException e) {
                return ResponseBuilder.getNotFoundResponse("unknown player");
            }
        }

        return returnNotFoundDefault();
    }

    private static class Response {

        final String name;
        final String uuid;
        final boolean namechanged;
        final Map<String, String> stats;

        Response(String name, String uuid, boolean nameChanged, Map<String, String> stats) {
            this.name = name;
            this.uuid = uuid;
            this.namechanged = nameChanged;
            this.stats = stats;
        }
    }
}
