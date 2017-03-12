package de.halfminer.hmr.rest;

import de.halfminer.hmr.HTTPServer;
import de.halfminer.hmr.interfaces.DELETECommand;
import de.halfminer.hmr.interfaces.GETCommand;
import de.halfminer.hmr.interfaces.POSTCommand;
import de.halfminer.hmr.interfaces.PUTCommand;
import fi.iki.elonen.NanoHTTPD;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by fabpw on 11.03.2017.
 */
@SuppressWarnings("unused")
public class Cmdstorage extends RESTCommand implements DELETECommand, GETCommand, POSTCommand, PUTCommand {

    @Override
    public NanoHTTPD.Response doOnDELETE() {
        //TODO implement deletion
        return returnNotFoundDefault();
    }

    @Override
    public NanoHTTPD.Response doOnGET() {
        if (uriParsed.meetsLength(1)) {
            String yamlPath = getYamlPathFromUri();
            //TODO remove from one parent up, (fix path)
            removeExpiredValues(yamlPath);
            return returnOK(Collections.singletonMap(uriParsed.getArgument(uriParsed.getLength()), storage.getString(yamlPath)));
        }

        return returnNotFoundDefault();
    }

    @Override
    public NanoHTTPD.Response doOnPOST() {
        //TODO disallow editing, only on PUT
        return doOnPUT();
    }

    @Override
    public NanoHTTPD.Response doOnPUT() {
        if (uriParsed.meetsLength(1) && bodyParsed.size() > 0) {

            String yamlPath = getYamlPathFromUri();
            removeExpiredValues(yamlPath);

            Map<String, String> oldValues = new HashMap<>();

            // default expiry of value in one hour
            long expiryTimestamp = (System.currentTimeMillis() / 1000) + 3600;
            boolean hasCreated = false;

            for (Map.Entry<String, String> pairToSet : bodyParsed.entrySet()) {

                if (pairToSet.getKey().equals("expiry")) {
                    try {
                        expiryTimestamp = Long.parseLong(pairToSet.getValue());
                    } catch (NumberFormatException ignored) {}
                }

                String path = yamlPath + pairToSet.getKey();
                String currentValue = storage.getString(path);
                oldValues.put(pairToSet.getKey(), currentValue);
                hasCreated = currentValue.length() == 0;
                storage.set(path, pairToSet.getValue());
            }

            storage.set(yamlPath + ".expiry", expiryTimestamp);
            NanoHTTPD.Response.Status responseStatus = hasCreated ?
                    NanoHTTPD.Response.Status.CREATED : NanoHTTPD.Response.Status.OK;

            oldValues.put("success", "true");
            NanoHTTPD.Response response = returnAnyStatus(responseStatus, oldValues);
            response.addHeader("Location", HTTPServer.lastHOST);
            return response;
        }

        return returnNotFoundDefault();
    }

    private String getYamlPathFromUri() {
        String yamlPath = "";
        for (String uriPart : uriParsed.getArguments()) {
            yamlPath += uriPart + '.';
        }
        return yamlPath;
    }

    private void removeExpiredValues(String path) {
        Object obj = storage.get(path);
        if (obj instanceof ConfigurationSection) {
            //TODO make recursive?
            ConfigurationSection sectionToCheck = (ConfigurationSection) obj;
            if (sectionToCheck.contains(path + ".expiry")) {
                long stamp = sectionToCheck.getLong(path + ".expiry");
                if (stamp < System.currentTimeMillis() / 1000) {
                    storage.set(path, null);
                }
            }
        }
    }
}
