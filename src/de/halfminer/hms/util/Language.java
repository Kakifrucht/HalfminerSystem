package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.ChatColor;
import org.bukkit.Location;

public class Language {

    private static final HalfminerSystem hms = HalfminerSystem.getInstance();

    /**
     * Get a message from the localization config
     *
     * @param messageKey key of the message, without "localization." as key
     * @param prefix     if true, adds a prefix
     * @return String containing the final and parsed message
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

    public static String placeholderReplace(String originalMessage, String... replacements) {
        if (replacements == null) return originalMessage;
        String toReturn = originalMessage;
        for (int i = 0; i < replacements.length; i += 2) {
            toReturn = toReturn.replace(replacements[i], replacements[i + 1]);
        }
        return toReturn;
    }

    public static String placeholderReplaceColor(String originalMessage, String... replacements) {
        return ChatColor.translateAlternateColorCodes('&', placeholderReplace(originalMessage, replacements));
    }

    public static String getMessagePlaceholderReplace(String messageKey, boolean prefix, String... replacements) {
        return placeholderReplace(getMessage(messageKey, prefix), replacements);
    }

    public static String getStringFromLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

}
