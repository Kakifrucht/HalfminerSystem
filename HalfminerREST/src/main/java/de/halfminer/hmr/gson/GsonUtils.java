package de.halfminer.hmr.gson;

import com.google.gson.GsonBuilder;

import java.util.Collections;
import java.util.Map;

/**
 * Utility class for {@link com.google.gson.Gson} library.
 */
public class GsonUtils {

    public static String returnPrettyJson(Object toSerialize) {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(toSerialize);
    }

    public static Map<String, String> getErrorMap(String errorMessage) {
        return Collections.singletonMap("error", errorMessage);
    }
}
