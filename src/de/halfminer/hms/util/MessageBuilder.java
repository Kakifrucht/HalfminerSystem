package de.halfminer.hms.util;

import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Class containing a builder used for messaging players / console
 */
@SuppressWarnings({"WeakerAccess", "SameParameterValue"})
public class MessageBuilder {

    /**
     * Create a new MessageBuilder
     * @param plugin plugin calling the builder
     * @param lang either the language key, or the message directly passed
     * @return MessageBuilder that can send a parsed message
     */
    public static MessageBuilder create(JavaPlugin plugin, String lang) {
        return new MessageBuilder(plugin, lang);
    }

    /**
     * Create a new MessageBuilder with a given prefix
     * @param plugin plugin calling the builder
     * @param lang either the language key, or the message directly passed
     * @param prefix prefix to be used
     * @return MessageBuilder that can send a parsed message
     */
    public static MessageBuilder create(JavaPlugin plugin, String lang, String prefix) {
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        return create(plugin, lang).setPrefix(prefix);
    }

    public static String returnMessage(JavaPlugin plugin, String lang) {
        return create(plugin, lang).returnMessage();
    }

    private final static char COLOR_CODE = '&';
    private final static char CLICKABLE_PREFIX = '~';
    private final static char PLACEHOLDER_CHARACTER = '%';

    private final JavaPlugin plugin;

    private final String lang;
    private String prefix;
    private Mode mode;
    private final Map<String, String> placeholders = new HashMap<>();

    private boolean makeCommandsClickable = true;
    private boolean startsWithClickableChar = false;
    private boolean loggingMode = false;

    private MessageBuilder(JavaPlugin plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.mode = Mode.GET_FROM_LOCALE_FILE;
    }

    public MessageBuilder setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    public MessageBuilder toggleClickableCommands() {
        makeCommandsClickable = !makeCommandsClickable;
        return this;
    }

    public MessageBuilder setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    /**
     * Adds a placeholder and what to replace it with to the message. The placeholder character % will be stripped.
     *
     * @param placeholder String to replace
     * @param replaceWith String with what to replace with
     * @return MessageBuilder, same instance
     */
    public MessageBuilder addPlaceholderReplace(String placeholder, String replaceWith) {
        placeholders.put(placeholder.replaceAll(PLACEHOLDER_CHARACTER + "", ""), replaceWith);
        return this;
    }

    public String returnMessage() {

        String toReturn;
        if (this.mode.equals(Mode.GET_FROM_LOCALE_FILE)) {
            toReturn = getMessage(lang);
            // allow removal of messages
            if (toReturn == null || toReturn.length() == 0)
                return "";
        } else {
            toReturn = lang;
        }

        if (makeCommandsClickable && toReturn.startsWith("" + CLICKABLE_PREFIX)) {
            toReturn = toReturn.substring(1);
            startsWithClickableChar = true;
        }

        if (prefix != null && !loggingMode) {
            toReturn = plugin.getConfig().getString("localization.prefix") + toReturn;
            this.addPlaceholderReplace("%PREFIX%", prefix);
        }

        toReturn = placeholderReplace(toReturn);
        toReturn = ChatColor.translateAlternateColorCodes(COLOR_CODE, toReturn).replace("\\n", "\n");
        if (loggingMode) toReturn = ChatColor.stripColor(toReturn);
        return toReturn;
    }

    public void sendMessage(CommandSender... sendToPlayers) {

        String messageToSend = returnMessage();

        for (CommandSender sendTo : sendToPlayers) {
            if (startsWithClickableChar && sendTo instanceof Player) {

                Player pSendTo = (Player) sendTo;
                pSendTo.spigot().sendMessage(makeCommandsClickable(messageToSend));
            } else sendTo.sendMessage(messageToSend);
        }
    }

    public void broadcastMessage(boolean log) {
        broadcastMessage(plugin.getServer().getOnlinePlayers(), log, "");
    }

    public void broadcastMessage(String permission, boolean log) {
        broadcastMessage(plugin.getServer().getOnlinePlayers(), log, permission);
    }

    public void broadcastMessage(Collection<? extends CommandSender> sendTo, boolean log, String permission) {

        sendTo.stream()
                .filter(player -> permission.length() == 0 || player.hasPermission(permission))
                .forEach(this::sendMessage);

        if (log) logMessage(Level.INFO);
    }

    public void logMessage(Level logLevel) {
        loggingMode = true;
        plugin.getLogger().log(logLevel, returnMessage());
        loggingMode = false;
    }

    private String getMessage(String messageKey) {
        return plugin.getConfig().getString("localization." + messageKey);
    }

    /**
     * Replace the placeholders from a given string with the given placeholders. The String will only be iterated once,
     * so the performance of this algorithm lies within O(n). Placeholders must start and end with character '%'.
     *
     * @param toReplace string containing the message that contains the placeholders
     * @return String containing the finished replaced message
     */
    private String placeholderReplace(String toReplace) {

        if (placeholders.size() == 0) return toReplace;

        StringBuilder message = new StringBuilder(toReplace);
        StringBuilder placeholder = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {

            // Go into placeholder read mode
            if (message.charAt(i) == PLACEHOLDER_CHARACTER) {

                // get the placeholder
                for (int j = i + 1; j < message.length() && message.charAt(j) != '%'; j++) {
                    placeholder.append(message.charAt(j));
                }

                // Do the replacement, add length of string to the outer loop index, since we do not want to iterate over
                // it again, or if no replacement was found, add the length of the read placeholder to skip it
                if (placeholders.containsKey(placeholder.toString())) {
                    String replaceWith = placeholders.get(placeholder.toString());
                    message.replace(i, i + placeholder.length() + 2, replaceWith);
                    i += replaceWith.length() - 1;
                } else i += placeholder.length() - 1;

                placeholder.setLength(0);
            }
        }
        return message.toString();
    }

    /**
     * Parses a string using the Component API ({@link net.md_5.bungee.api.chat.BaseComponent}).
     * Commands will be made clickable and italic, adding a hover message to the command. Commands
     * must be incapsulated with trailing '/' character.
     *
     * @param text string that will be parsed
     * @return TextComponent that is ready to be sent to a {@link org.bukkit.entity.Player}
     */
    private TextComponent makeCommandsClickable(String text) {

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
                                new ComponentBuilder(getMessage("cmdCustomtextClick")).create()));
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

    public enum Mode {
        GET_FROM_LOCALE_FILE,
        DIRECT_STRING
    }
}
