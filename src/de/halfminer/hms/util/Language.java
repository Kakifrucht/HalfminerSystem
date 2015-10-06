package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.ChatColor;
import org.bukkit.Location;

public class Language {

    private static final HalfminerSystem hms = HalfminerSystem.getInstance();

    /**
     * Get a message from the localization config, while translating color codes, adding newlines and adding a prefix
     *
     * @param messageKey key of the message, without "localization." as key
     * @param prefix     if true, adds the prefix
     * @return String containing the message with prefix or not, converted newlines and translated color codes
     */
    public static String getMessage(String messageKey, boolean prefix) {

        String toReturn = hms.getConfig().getString("localization." + messageKey);
        if (toReturn == null || toReturn.length() == 0) return ""; //Allow messages to be removed
        //Get proper color codes and newlines, add prefix
        toReturn = ChatColor.translateAlternateColorCodes('&', toReturn).replace("\\n", "\n");

        if (prefix)
            toReturn = ChatColor.translateAlternateColorCodes('&', hms.getConfig().getString("localization.prefix")) + toReturn;

        return toReturn;
    }

    /**
     * Replace the placeholders from a given string with the given placeholders
     *
     * @param originalMessage string containing the message that contains the placeholders
     * @param replacements    array containing as even index the placeholder and odd index with what to replace it
     * @return String containing the finished replaced message
     */
    public static String placeholderReplace(String originalMessage, String... replacements) {
        if (replacements == null) return originalMessage;
        String toReturn = originalMessage;
        for (int i = 0; i < replacements.length; i += 2) {
            toReturn = toReturn.replace(replacements[i], replacements[i + 1]);
        }
        return toReturn;
    }

    /**
     * Replaces the placeholders from a given string while translating the color codes
     *
     * @param originalMessage string containing the message that contains the placeholders and untranslated color codes
     * @param replacements    array containing as even index the placeholder and odd index with what to replace it
     * @return String containing the finished replaced and translated message
     */
    public static String placeholderReplaceColor(String originalMessage, String... replacements) {
        return ChatColor.translateAlternateColorCodes('&', placeholderReplace(originalMessage, replacements));
    }

    /**
     * Get a message from the localization config, while translating color codes, adding newlines and adding a prefix,
     * and replacing the placeholders at the same time
     *
     * @param messageKey   key of the message, without "localization." as key
     * @param prefix       if true, will add the prefix from the config
     * @param replacements array containing as even index the placeholder and odd index with what to replace it
     * @return String containing the finished message
     */
    public static String getMessagePlaceholderReplace(String messageKey, boolean prefix, String... replacements) {
        return placeholderReplace(getMessage(messageKey, prefix), replacements);
    }

    /**
     * Returns a string representation of a location, using block coordinates (only full coords, no floats)
     *
     * @param loc Location to get a String from
     * @return String in the format 'x, y, z'
     */
    public static String getStringFromLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

}
