package de.halfminer.hmr.gson;

import com.google.gson.GsonBuilder;

/**
 * Utility class for {@link com.google.gson.Gson} library.
 */
public class GsonUtils {

    public static String returnPrettyJson(Object toSerialize) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(toSerialize);
    }

    public static String returnErrorJson(String errorMessage) {
        return returnPrettyJson(new Error(errorMessage));
    }

    @SuppressWarnings("unused")
    private static class Error {

        final String error;

        Error(String error) {
            this.error = error;
        }
    }
}
