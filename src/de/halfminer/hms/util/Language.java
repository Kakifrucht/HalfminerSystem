package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Class containing language related static methods
 */
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

            // Go into placeholder read mode
            if (message.charAt(i) == '%') {

                // get the placeholder
                placeholder.append('%');
                for (int j = i + 1; j < message.length() && message.charAt(j) != '%'; j++) {
                    placeholder.append(message.charAt(j));
                }
                placeholder.append('%');

                // get the string that will replace the placeholder
                String replaceWith = null;
                for (int j = 0; j < replacements.length; j += 2) {
                    if (replacements[j].equals(placeholder.toString())) {
                        replaceWith = replacements[j + 1];
                        break;
                    }
                }

                // Do the replacement, add length of string to the outer loop index, since we do not want to iterate over
                // it again, or if no replacement was found, add the length of the read placeholder to skip it
                if (replaceWith != null) {
                    message.replace(i, i + placeholder.length(), replaceWith);
                    i += replaceWith.length() - 1;
                } else i += placeholder.length() - 1;

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
     * Takes an array and creates the string with spaces between entries, starting from given startIndex
     *
     * @param array Array to convert
     * @param startIndex int to start from
     * @param translateColor if true, will also translate color codes &
     * @return String
     */
    public static String arrayToString(String[] array, int startIndex, boolean translateColor) {
        return arrayToString(array, startIndex, array.length, translateColor);
    }

    /**
     * Takes an array and creates the string with spaces between entries, starting from given startIndex
     * until given endIndex
     *
     * @param array Array to convert
     * @param startIndex int to start from
     * @param endIndex int to end
     * @param translateColor if true, will also translate color codes &
     * @return String
     */
    public static String arrayToString(String[] array, int startIndex, int endIndex, boolean translateColor) {

        if (startIndex >= endIndex) throw new IllegalArgumentException
                ("startIndex (" + startIndex + ") must be smaller then endIndex(" + endIndex + ")");

        String toReturn = "";
        if (array.length == 0) return toReturn;
        for (int i = startIndex; i < array.length && i < endIndex; i++) {
            toReturn += array[i] + ' ';
        }
        if (translateColor) toReturn = ChatColor.translateAlternateColorCodes('&', toReturn);
        return toReturn.substring(0, toReturn.length() - 1);
    }

    /**
     * Takes a String, lowercases it, exchanges the first character with its uppercase equivalent and transforms
     * underscores to spaces. "SUGAR_CANE" will be "Sugar cane", "tEsT tHiS" would be "Test this".
     *
     * @param str String that should be transformed
     * @return transformed string
     */
    public static String makeStringFriendly(String str) {
        if (str == null || str.length() == 0) return "";
        String toReturn = str.toLowerCase().replace('_', ' ');

        return Character.toUpperCase(toReturn.charAt(0)) + toReturn.substring(1);
    }

    /**
     * Determines if the message starts with '~' and recipient can receive json messages,
     * if yes, parse the message (via {@link #makeCommandsClickable(String)}),
     * else just send unparsed, however remove leading '~' character.
     *
     * @param sendTo recipient for the message
     * @param toParseAndSend message that will be send and/or parsed
     */
    public static void sendParsedText(CommandSender sendTo, String toParseAndSend) {

        if (sendTo instanceof Player && toParseAndSend.startsWith("~")) {
            ((Player) sendTo).spigot().sendMessage(makeCommandsClickable(toParseAndSend.substring(1)));
        } else {

            String send = toParseAndSend.startsWith("~") ? toParseAndSend.substring(1) : toParseAndSend;
            sendTo.sendMessage(send);
        }
    }

    /**
     * Parses a string using the Component API ({@link net.md_5.bungee.api.chat.BaseComponent}).
     * Commands will be made clickable and italic, adding a hover message to the command. Commands
     * must be incapsulated with trailing '/' character.
     *
     * @param text string that will be parsed
     * @return TextComponent that is ready to be sent to a {@link org.bukkit.entity.Player}
     */
    private static TextComponent makeCommandsClickable(String text) {

        TextComponent parsedComponent = new TextComponent();
        List<BaseComponent> components = new ArrayList<>();

        for (BaseComponent baseComp : TextComponent.fromLegacyText(text)) {

            if (!(baseComp instanceof TextComponent)) {
                components.add(baseComp);
                continue;
            }

            TextComponent currentComponent = (TextComponent) baseComp;

            // init new textcomponent with same attributes as original one
            TextComponent currentComp = new TextComponent(currentComponent);
            int currentLowerBound = 0;

            StringBuilder originalText = new StringBuilder(currentComponent.getText());
            boolean readingCommand = false;

            for (int i = 0; i < originalText.length(); i++) {

                if (originalText.charAt(i) == '/') {

                    if (readingCommand) {

                        String command = originalText.substring(currentLowerBound, i);

                        currentComp.setText(command);
                        currentComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                        currentComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder(Language.getMessage("cmdCustomtextClick")).create()));
                        // every command is italic
                        currentComp.setItalic(true);
                        // remove closing slash from text
                        originalText.deleteCharAt(i);

                    } else currentComp.setText(originalText.substring(currentLowerBound, i));

                    readingCommand = !readingCommand;
                    components.add(currentComp);
                    currentComp = new TextComponent(currentComponent);
                    currentLowerBound = i;
                }
            }

            // add last component to list
            if (currentLowerBound != originalText.length()) {
                currentComp.setText(originalText.substring(currentLowerBound, originalText.length()));
                components.add(currentComp);
            }
        }

        components.forEach(parsedComponent::addExtra);
        return parsedComponent;
    }
}
