package de.halfminer.hms.util;

import net.md_5.bungee.api.chat.*;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * Class containing a builder used for messaging players / console
 */
@SuppressWarnings("ALL")
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
        return create(plugin, lang).setPrefix(prefix);
    }

    public static String returnMessage(JavaPlugin plugin, String lang) {
        return create(plugin, lang).returnMessage();
    }

    private final static char colorCode = '&';
    private final static char clickablePrefix = '~';

    private final JavaPlugin plugin;

    private final String lang;
    private String prefix;
    private MessageMode mode;
    private final List<String> placeholders = new ArrayList<>();

    private boolean translateColor = true;
    private boolean makeCommandsClickable = true;
    private boolean startsWithClickableChar = false;

    private MessageBuilder(JavaPlugin plugin, String lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.mode = MessageMode.GET_FROM_LOCALE_FILE;
    }

    public MessageBuilder setMode(MessageMode mode) {
        this.mode = mode;
        return this;
    }

    public MessageBuilder toggleColorTranslate() {
        translateColor = !translateColor;
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

    public MessageBuilder addPlaceholderReplace(String placeholder, String replaceWith) {
        placeholders.add(placeholder);
        placeholders.add(replaceWith);
        return this;
    }

    public String returnMessage() {

        String toReturn;
        if (this.mode.equals(MessageMode.GET_FROM_LOCALE_FILE)) {
            toReturn = getMessage(lang);
            // allow removal of messages
            if (toReturn == null || toReturn.length() == 0)
                return "";
        } else {
            toReturn = lang;
        }

        if (makeCommandsClickable && toReturn.startsWith("" + clickablePrefix)) {
            toReturn = toReturn.substring(1);
            startsWithClickableChar = true;
        }

        if (prefix != null) {
            toReturn = plugin.getConfig().getString("localization.prefix") + toReturn;
            this.addPlaceholderReplace("%PREFIX%", prefix);
        }

        if (translateColor) toReturn = ChatColor.translateAlternateColorCodes(colorCode, toReturn);
        toReturn = toReturn.replace("\\n", "\n");

        toReturn = placeholderReplace(toReturn);

        return toReturn;
    }

    public void sendMessage(CommandSender sendTo) {

        String messageToSend = returnMessage();

        if (startsWithClickableChar && sendTo instanceof Player) {

            Player pSendTo = (Player) sendTo;
            pSendTo.spigot().sendMessage(makeCommandsClickable(messageToSend));
        } else sendTo.sendMessage(messageToSend);
    }

    public void broadcastMessage(boolean log) {
        broadcastMessage(plugin.getServer().getOnlinePlayers(), log);
    }

    public void broadcastMessage(Collection<? extends CommandSender> sendTo, boolean log) {

        sendTo.forEach(this::sendMessage);
        if (log) plugin.getLogger().info(returnMessage());
    }

    public void logMessage(Level logLevel) {
        plugin.getLogger().log(logLevel, returnMessage());
    }

    private String getMessage(String messageKey) {
        return plugin.getConfig().getString("localization." + messageKey);
    }

    private String placeholderReplace(String toReplace) {

        if (placeholders.size() == 0) return toReplace;

        StringBuilder message = new StringBuilder(toReplace);
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
                for (int j = 0; j < placeholders.size(); j += 2) {
                    if (placeholders.get(j).equals(placeholder.toString())) {
                        replaceWith = placeholders.get(j + 1);
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

    public enum MessageMode {
        GET_FROM_LOCALE_FILE,
        DIRECT_STRING
    }
}