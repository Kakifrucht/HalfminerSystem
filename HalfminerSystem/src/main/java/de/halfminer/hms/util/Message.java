package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.logging.Level;

/**
 * Class wrapping a builder like object for messaging purposes.
 * To access use it's static {@link #create(String, Plugin, String)} method.
 * To use the default plugins locale section (HalfminerSystem) the plugin parameter can be ommited.
 * Translations will be pulled from the supplied plugins config.yml "localization" section,
 * or if {@link #setDirectString()} is set to true they can also be hardcoded and passed
 * directly as parameter instead of the message key.
 *
 * Message color will be filtered if message is printed to console, otherwise {@link #COLOR_CODE}
 * character will be replaced automatically with the actual color code for Minecraft.
 *
 * If messages in config start with {@link #CLICKABLE_PREFIX} character and the message
 * is sent to a player, the plugin will make commands in the message (encapsulated with the
 * {@link #COMMAND_PREFIX} character) clickable.
 *
 * There are also shortcuts to just quickly return a message string in one line,
 * use the static {@link #returnMessage(String, Plugin, boolean)} methods (and overloads).
 */
public class Message {

    /**
     * Create a new MessageBuilder with default plugin
     *
     * @param lang either the language key, or the message directly passed
     * @return MessageBuilder that can send a parsed message
     */
    public static Message create(String lang) {
        return create(lang, HalfminerSystem.getInstance());
    }

    /**
     * Create a new MessageBuilder with default plugin and %PREFIX% placeholder replaced
     *
     * @param lang either the language key, or the message directly passed
     * @param prefix %PREFIX% placeholder to be added
     * @return MessageBuilder that can send a parsed message
     */
    public static Message create(String lang, String prefix) {
        return create(lang, HalfminerSystem.getInstance(), prefix);
    }

    /**
     * Create a new MessageBuilder with a given prefix
     *
     * @param lang either the language key, or the message directly passed
     * @param plugin plugin calling the builder or null to use default API locale keys
     * @param prefix %PREFIX% placeholder to be added
     * @return MessageBuilder that can send a parsed message
     */
    public static Message create(String lang, Plugin plugin, String prefix) {
        return create(lang, plugin).addPlaceholder("PREFIX", prefix);
    }

    /**
     * Create a new MessageBuilder
     *
     * @param lang either the language key, or the message directly passed
     * @param plugin plugin calling the builder or null to use default API locale keys
     * @return MessageBuilder that can send a parsed message
     */
    public static Message create(String lang, Plugin plugin) {
        return new Message(plugin, lang);
    }

    public static String returnMessage(String lang) {
        return returnMessage(lang, null, true);
    }

    public static String returnMessage(String lang, Plugin plugin) {
        return returnMessage(lang, plugin, true);
    }

    public static String returnMessage(String lang, Plugin plugin, boolean usePrefix) {
        Message builder = create(lang, plugin);
        if (!usePrefix) builder.togglePrefix();
        return builder.returnMessage();
    }

    private static final char COLOR_CODE = '&';
    private static final char CLICKABLE_PREFIX = '~';
    private static final char PLACEHOLDER_CHARACTER = '%';
    private static final char COMMAND_PREFIX = '/';

    private final Plugin plugin;

    private final String lang;
    private final Map<String, String> placeholders = new HashMap<>();

    private boolean getFromLocale = true;
    private boolean usePrefix = true;
    private boolean startsWithClickableChar = false;

    private Message(Plugin plugin, String lang) {
        this.plugin = plugin != null ? plugin : HalfminerSystem.getInstance();
        this.lang = lang;
    }

    public Message setDirectString() {
        getFromLocale = false;
        usePrefix = false;
        return this;
    }

    public Message togglePrefix() {
        usePrefix = !usePrefix;
        return this;
    }

    /**
     * Adds a placeholder and what to replace it with to the message. The {@link #PLACEHOLDER_CHARACTER} will be stripped.
     *
     * @param placeholder String to replace
     * @param replaceWith String with what to replace with
     * @return MessageBuilder, same instance
     */
    public Message addPlaceholder(String placeholder, String replaceWith) {
        placeholders.put(placeholder.replaceAll(PLACEHOLDER_CHARACTER + "", "").trim(), replaceWith);
        return this;
    }

    public Message addPlaceholder(String placeholder, Object replaceWith) {
        addPlaceholder(placeholder, String.valueOf(replaceWith));
        return this;
    }

    public String returnMessage() {
        return returnMessage(false);
    }

    private String returnMessage(boolean loggingMode) {

        String toReturn;
        if (getFromLocale) {
            toReturn = getMessageFromLocale(lang);
            // allow removal of messages
            if (toReturn == null || toReturn.length() == 0)
                return "";
        } else {
            toReturn = lang;
        }

        if (toReturn.startsWith("" + CLICKABLE_PREFIX)) {
            toReturn = toReturn.substring(1);
            startsWithClickableChar = true;
        }

        if (usePrefix && !loggingMode) {
            String prefixPlaceholder = getMessageFromLocale("prefix");
            // if %PREFIX% placeholder is part of plugins prefix, check if MessageBuilder contains
            // said placeholder, only add prefix if placeholder will be replaced
            if (prefixPlaceholder.length() > 0
                    && (!prefixPlaceholder.contains("%PREFIX%") || placeholders.containsKey("PREFIX"))) {
                toReturn = prefixPlaceholder + toReturn;
            }
        }

        toReturn = doPlaceholderReplace(toReturn);
        toReturn = ChatColor.translateAlternateColorCodes(COLOR_CODE, toReturn).replace("\\n", "\n");

        // if logging, remove color codes and remove encapsulating COMMAND_PREFIX if necessary
        if (loggingMode) {
            toReturn = ChatColor.stripColor(toReturn);
            if (startsWithClickableChar) {
                StringBuilder removeSlash = new StringBuilder(toReturn);
                boolean removeNextSlash = false;
                for (int i = 0; i < removeSlash.length(); i++) {
                    if (removeSlash.charAt(i) == COMMAND_PREFIX) {
                        if (removeNextSlash) removeSlash.deleteCharAt(i);
                        removeNextSlash = !removeNextSlash;
                    }
                }
                toReturn = removeSlash.toString();
            }
        }
        return toReturn;
    }

    public void send(CommandSender... sendToPlayers) {

        String messageToSend = returnMessage();

        if (messageToSend.length() > 0 || !getFromLocale) {
            for (CommandSender sendTo : sendToPlayers) {
                if (startsWithClickableChar && sendTo instanceof Player) {

                    Player pSendTo = (Player) sendTo;
                    pSendTo.spigot().sendMessage(makeCommandsClickable(messageToSend));
                } else if (sendTo instanceof Player) {
                    sendTo.sendMessage(messageToSend);
                } else {
                    sendTo.sendMessage(returnMessage(true));
                }
            }
        }
    }

    public void broadcast(boolean log) {
        broadcast(plugin.getServer().getOnlinePlayers(), log, "");
    }

    public void broadcast(String permission, boolean log) {
        broadcast(plugin.getServer().getOnlinePlayers(), log, permission);
    }

    public void broadcast(Collection<? extends CommandSender> sendTo, boolean log, String permission) {

        sendTo.stream()
                .filter(player -> permission.length() == 0 || player.hasPermission(permission))
                .forEach(this::send);

        if (log) log(Level.INFO);
    }

    public void log(Level logLevel) {
        String toLog = returnMessage(true);
        if (toLog.length() > 0)
            plugin.getLogger().log(logLevel, toLog);
    }

    private String getMessageFromLocale(String messageKey) {
        return plugin.getConfig().getString("localization." + messageKey, "");
    }

    /**
     * Replace the placeholders from a given string with the given placeholders. The String will only be iterated once,
     * so the performance of this algorithm lies within O(n). Placeholders must start and end with character '%'.
     *
     * @param toReplace string containing the message that contains the placeholders
     * @return String containing the finished replaced message
     */
    private String doPlaceholderReplace(String toReplace) {

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
                } else i += placeholder.length();

                placeholder.setLength(0);
            }
        }
        return message.toString();
    }

    /**
     * Parses a string using the Component API ({@link net.md_5.bungee.api.chat.BaseComponent}).
     * Commands will be made clickable and italic, adding a hover message to the command. Commands
     * must be incapsulated with trailing {@link #COMMAND_PREFIX} character.
     *
     * @param text string that will be parsed
     * @return TextComponent that is ready to be sent to a {@link org.bukkit.entity.Player}
     */
    private TextComponent makeCommandsClickable(String text) {

        List<BaseComponent> components = new ArrayList<>();
        for (BaseComponent baseComp : TextComponent.fromLegacyText(text)) {

            if (!(baseComp instanceof TextComponent)) {
                components.add(baseComp);
                continue;
            }

            TextComponent currentComponent = (TextComponent) baseComp;

            // init new textcomponent with same attributes as original one
            TextComponent newComponent = new TextComponent(currentComponent);
            int currentLowerBound = 0;

            StringBuilder originalText = new StringBuilder(currentComponent.getText());
            if (originalText.toString().startsWith("http")) {
                components.add(newComponent);
                continue;
            }

            boolean readingCommand = false;

            for (int i = 0; i < originalText.length(); i++) {

                if (originalText.charAt(i) == COMMAND_PREFIX) {

                    if (readingCommand) {

                        String command = originalText.substring(currentLowerBound, i);

                        newComponent.setText(command);
                        newComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

                        String clickHoverMessage = Message.returnMessage("commandClickHover");
                        if (clickHoverMessage.length() > 0) {
                            newComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new ComponentBuilder(clickHoverMessage).create()));
                        }

                        // every command is italic
                        newComponent.setItalic(true);
                        // remove closing slash from text
                        originalText.deleteCharAt(i);

                    } else newComponent.setText(originalText.substring(currentLowerBound, i));

                    readingCommand = !readingCommand;
                    components.add(newComponent);
                    newComponent = new TextComponent(currentComponent);
                    currentLowerBound = i;
                }
            }

            // add last component to list
            if (currentLowerBound != originalText.length()) {
                newComponent.setText(originalText.substring(currentLowerBound, originalText.length()));
                components.add(newComponent);
            }
        }

        TextComponent newParsedComponent = new TextComponent();
        components.forEach(newParsedComponent::addExtra);
        return newParsedComponent;
    }
}
