package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;

public class Language {

    private static final HalfminerSystem hms = HalfminerSystem.getInstance();

    /**
     * Get a message from the localization config, while translating color codes and adding newlines
     *
     * @param messageKey key of the message, without "localization." as key
     * @return String containing the message with prefix or not, converted newlines and translated color codes
     */
    public static String getMessage(String messageKey) {

        String toReturn = hms.getConfig().getString("localization." + messageKey);
        if (toReturn == null || toReturn.length() == 0) return ""; //Allow messages to be removed
        //Get proper color codes and newlines, add prefix
        toReturn = ChatColor.translateAlternateColorCodes('&', toReturn).replace("\\n", "\n");

        return toReturn;
    }

    /**
     * Replace the placeholders from a given string with the given placeholders. The String will only be iterated once,
     * so the performance of this algorithm lies within O(n). Placeholders must start and end with character '%'.
     *
     * @param originalMessage string containing the message that contains the placeholders
     * @param replacements    array containing as even index the placeholder in format %PLACEHOLDER%
     *                        and odd index the string that will replace the placeholder
     * @return String containing the finished replaced message
     */
    public static String placeholderReplace(String originalMessage, String... replacements) {

        if (replacements == null) return originalMessage;

        StringBuilder message = new StringBuilder(originalMessage);
        StringBuilder placeholder = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {

            //Go into placeholder read mode
            if (message.charAt(i) == '%') {

                //get the placeholder
                placeholder.append('%');
                for (int j = i + 1; j < message.length() && message.charAt(j) != '%'; j++) {
                    placeholder.append(message.charAt(j));
                }
                placeholder.append('%');

                //get the string that will replace the placeholder
                String replaceWith = null;
                for (int j = 0; j < replacements.length; j += 2) {
                    if (replacements[j].equals(placeholder.toString())) {
                        replaceWith = replacements[j + 1];
                        break;
                    }
                }

                //Do the replacement, add length of string to the outer loop index, since we do not want to iterate over
                //it again, or if no replacement was found, add the length of the read placeholder to skip it
                if (replaceWith != null) {
                    message.replace(i, i + placeholder.length(), replaceWith);
                    i += replaceWith.length();
                } else i += placeholder.length();

                placeholder.setLength(0);
            }
        }
        return message.toString();
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
    public static String getMessagePlaceholders(String messageKey, boolean prefix, String... replacements) {
        String toReturn = getMessage(messageKey);

        if (prefix)
            toReturn = ChatColor.translateAlternateColorCodes('&', hms.getConfig().getString("localization.prefix")) + toReturn;

        return placeholderReplace(toReturn, replacements);
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

    /**
     * Takes an array and creates the string with spaces between entries, starting from given index
     *
     * @param args           Array to convert
     * @param index          int to start from
     * @param translateColor if true, will also translate color codes &
     * @return String
     */
    public static String arrayToString(String[] args, int index, boolean translateColor) {
        String toReturn = "";
        if (args.length == 0) return toReturn;
        for (int i = index; i < args.length; i++) {
            toReturn += args[i] + ' ';
        }
        if (translateColor) toReturn = ChatColor.translateAlternateColorCodes('&', toReturn);
        return toReturn.substring(0, toReturn.length() - 1);
    }

    /**
     * Takes a material, lowercases it, exchanges the first character with its uppercase equivalent and transforms
     * underscores to spaces. Material.SUGAR_CANE will be "Sugar cane".
     *
     * @param mat material that should be transformed
     * @return transformed string
     */
    public static String makeMaterialStringFriendly(Material mat) {
        if (mat == null) return "";
        String toReturn = mat.toString().toLowerCase().replace('_', ' ');

        return Character.toUpperCase(toReturn.charAt(0)) + toReturn.substring(1);
    }

}
