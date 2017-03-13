package de.halfminer.hmr.rest;

import de.halfminer.hmr.HTTPServer;
import de.halfminer.hmr.gson.GsonUtils;
import de.halfminer.hmr.interfaces.DELETECommand;
import de.halfminer.hmr.interfaces.GETCommand;
import de.halfminer.hmr.interfaces.POSTCommand;
import de.halfminer.hmr.interfaces.PUTCommand;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * - Data creation/modification/retrieval, where URI is the path to the given resource
 * - *DELETE* /< path[/...]>[content:key&...]
 *   - Delete a whole section or just the values at the supplied keys
 * - *GET* /< path[/...]>[?:key&...]
 *   - Get the whole section or just the values at the supplied keys
 * - *PUT/POST* /< path[/...]>[content:key=value&...&expiry=seconds]
 *   - Add data to the given path, supplied via content body as *application/x-www-form-urlencoded*
 *     - POST only for creation, not modification, PUT for both
 *   - Expiry timestamp can be passed as part of the content body, otherwise default of one hour will be used
 */
@SuppressWarnings("unused")
public class Cmdstorage extends RESTCommand implements DELETECommand, GETCommand, POSTCommand, PUTCommand {

    private String basePath = "";

    @Override
    protected boolean doForAll() {
        if (uriParsed.meetsLength(1)) {

            for (String uriPart : uriParsed.getArguments()) {
                basePath += uriPart + '.';
            }

            basePath = basePath.substring(0, basePath.length() - 1);

            long expiryStamp = storage.getLong(basePath + ".expiry");
            if (expiryStamp > 0 && expiryStamp < System.currentTimeMillis() / 1000) {
                storage.set(basePath, null);
            }
            return true;
        } else return false;
    }

    @Override
    public NanoHTTPD.Response doOnDELETE() {

        // delete whole section or supplied keys
        if (bodyParsed.size() == 0) {
            if (storage.get(basePath) instanceof ConfigurationSection) {
                storage.set(basePath, null);
                return returnOK(Collections.singletonMap(basePath, "section deleted"));
            } else {
                return returnNotFound(GsonUtils.getErrorMap("path is not a section and no keys were specified"));
            }
        } else {
            boolean hasDeleted = false;
            Map<String, String> deleted = new HashMap<>();
            for (String key : bodyParsed.keySet()) {
                String currentPath = basePath + '.' + key;
                String oldVal = storage.getString(currentPath);

                hasDeleted = oldVal.length() > 0;
                storage.set(currentPath, null);
                deleted.put(currentPath, oldVal);
            }
            return returnAnyStatus(hasDeleted ?
                    NanoHTTPD.Response.Status.OK : NanoHTTPD.Response.Status.NOT_FOUND, deleted);
        }
    }

    @Override
    public NanoHTTPD.Response doOnGET() {

        // try to iterate over a section if no GET parameters were supplied
        Set<String> keysToIterate;
        if (paramsParsed.size() > 0) {
            keysToIterate = paramsParsed.keySet();
        } else {
            Object get = storage.get(basePath);
            if (get instanceof ConfigurationSection) {
                keysToIterate = ((ConfigurationSection) get).getKeys(false);
            } else {
                return returnNotFound(GsonUtils.getErrorMap("path is not a section, specify keys to get"));
            }
        }

        boolean hasFoundValue = false;
        Map<String, String> toReturn = new HashMap<>();
        for (String key : keysToIterate) {

            if (key.equals("expiry")) continue;
            String currentPath = basePath + '.' + key;

            Object get = storage.get(currentPath);
            if (get != null && !(get instanceof ConfigurationSection)) {
                String value = get.toString();
                hasFoundValue = value.length() > 0;
                toReturn.put(currentPath, value);
            }
        }

        return returnAnyStatus(
                hasFoundValue ? NanoHTTPD.Response.Status.OK : NanoHTTPD.Response.Status.NOT_FOUND,
                toReturn.size() > 0 ? toReturn : GsonUtils.getErrorMap("not found"));
    }

    @Override
    public NanoHTTPD.Response doOnPOST() {
        //TODO disallow editing, only on PUT
        return doOnPUT();
    }

    @Override
    public NanoHTTPD.Response doOnPUT() {

        if (bodyParsed.size() > 0) {
            Map<String, String> toReturn = new HashMap<>();

            // default expiry of value in one hour
            long expiryTimestamp = (System.currentTimeMillis() / 1000) + 3600;
            boolean hasCreated = false;

            for (Map.Entry<String, String> pairToSet : bodyParsed.entrySet()) {

                if (pairToSet.getKey().equals("expiry")) {
                    try {
                        expiryTimestamp = (System.currentTimeMillis() / 1000) + Long.parseLong(pairToSet.getValue());
                        continue;
                    } catch (NumberFormatException ignored) {}
                }

                String currentPath = basePath + '.' + pairToSet.getKey();
                String currentValue = storage.getString(currentPath);

                toReturn.put(currentPath, currentValue);
                hasCreated = currentValue.length() == 0;

                storage.set(currentPath, pairToSet.getValue());
            }

            storage.set(basePath + ".expiry", expiryTimestamp);

            NanoHTTPD.Response response = returnAnyStatus(hasCreated ?
                    NanoHTTPD.Response.Status.CREATED : NanoHTTPD.Response.Status.OK, toReturn);
            response.addHeader("Location", HTTPServer.lastHOST);
            return response;
        }

        return returnNotFoundDefault();
    }
}
