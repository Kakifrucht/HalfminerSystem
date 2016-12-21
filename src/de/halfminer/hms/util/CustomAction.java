package de.halfminer.hms.util;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.exception.FormattingException;
import de.halfminer.hms.exception.GiveItemException;
import de.halfminer.hms.interfaces.CacheHolder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Action that will be read from a {@link CustomtextCache}, parsed and run. Supported actions are defined in
 * {@link Type}. Actions can define a minimum requirement for players necessary for the action to be run, which
 * will then have to be passed to {@link #runAction(Player...)}. Additional placeholders for the next run can be
 * defined via {@link #addPlaceholderForNextRun(String, String)}.
 */
@SuppressWarnings("SameParameterValue")
public class CustomAction {

    private final String actionName;
    private final JavaPlugin plugin;
    private final CustomitemCache itemCache;
    private final List<Pair<Type, String>> parsedActionList;

    private int playersRequired = 1;

    private final Map<String, String> nextRunPlaceholders = new HashMap<>();

    /**
     * Create a new action. If the action name is "nothing", the action won't do anything on run.
     *
     * @param action String naming the action
     * @param plugin plugin creating the action
     * @param holder class holding necessary caches (customactions.txt and customitems.txt)
     * @throws CachingException if the action could not be found or the caches could not be
     *                          initialized due to {@link java.io.IOException}
     */
    public CustomAction(String action, JavaPlugin plugin, CacheHolder holder) throws CachingException {

        this.actionName = action.toLowerCase();
        this.plugin = plugin;
        this.itemCache = new CustomitemCache(plugin, holder.getCache("customitems.txt"));
        CustomtextCache actionCache = holder.getCache("customactions.txt");

        if (action.equalsIgnoreCase("nothing")) {
            this.parsedActionList = null;
            return;
        } else this.parsedActionList = new ArrayList<>();

        List<String> actionParsed = actionCache.getChapter(actionName);

        for (int lineNumber = 0; lineNumber < actionParsed.size(); lineNumber++) {

            String line = actionParsed.get(lineNumber);

            Pair<String, String> keyValuePair;
            try {
                keyValuePair = Utils.getKeyValuePair(line);
            } catch (FormattingException e) {
                logError("FORMAT", lineNumber);
                continue;
            }

            Type type = Type.getFromString(keyValuePair.getLeft());
            if (type != null) {
                if (type.equals(Type.REQUIRED_PLAYERS)) {
                    try {
                        playersRequired = Integer.parseInt(keyValuePair.getRight());
                        playersRequired = Math.max(playersRequired, 1);
                    } catch (NumberFormatException e) {
                        logError("PLAYERSREQUIRED", lineNumber);
                    }
                } else parsedActionList.add(new Pair<>(type, keyValuePair.getRight()));
            } else {
                logError("ACTIONTYPE", lineNumber);
            }
        }
    }

    public void runAction(Player... players) {

        // support empty action
        if (parsedActionList == null) return;

        if (players.length < playersRequired) {
            logError("NOTENOUGHPLAYERS", 0);
            return;
        }

        for (Pair<Type, String> action : parsedActionList) {

            MessageBuilder parsedMessage = replaceWithPlaceholders(action.getRight(), players);

            switch (action.getLeft()) {
                case CONSOLE_COMMAND:
                    boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            parsedMessage.returnMessage());
                    if (!success) {
                        MessageBuilder.create(plugin, "utilCustomActionCommandNotFound")
                                .addPlaceholderReplace("%COMMAND%", parsedMessage.returnMessage())
                                .logMessage(Level.WARNING);
                    }
                    break;
                case GIVE_CUSTOM_ITEM:
                    try {
                        StringArgumentSeparator separator = new StringArgumentSeparator(parsedMessage.returnMessage());
                        Map<String, String> allPlaceholders = new HashMap<>();
                        allPlaceholders.putAll(nextRunPlaceholders);
                        allPlaceholders.putAll(getPlayerPlaceholderMap(players));
                        itemCache.giveItem(parsedMessage.returnMessage(), players[0],
                                separator.getArgumentIntMinimum(1, 1), allPlaceholders);
                    } catch (GiveItemException e) {

                        if (!e.getReason().equals(GiveItemException.Reason.INVENTORY_FULL)) {
                            MessageBuilder.create(plugin, "utilCustomActionGiveItemError")
                                    .addPlaceholderReplace("%ITEM%", parsedMessage.returnMessage())
                                    .addPlaceholderReplace("%REASON%", e.getCleanReason())
                                    .logMessage(Level.WARNING);
                        }
                        // quit execution of action on fail
                        return;
                    }
                    break;
                case BROADCAST:
                case TELL:
                    if (action.getLeft() == Type.BROADCAST) parsedMessage.broadcastMessage(true);
                    else parsedMessage.sendMessage(players[0]);
            }
        }
        nextRunPlaceholders.clear();
    }

    public void addPlaceholderForNextRun(String placeholder, String replaceWith) {
        nextRunPlaceholders.put(placeholder, replaceWith);
    }

    private MessageBuilder replaceWithPlaceholders(String toReplace, Player[] players) {

        MessageBuilder message = MessageBuilder.create(plugin, toReplace).setMode(MessageBuilder.Mode.DIRECT_STRING);

        nextRunPlaceholders.entrySet()
                .forEach(entry -> message.addPlaceholderReplace(entry.getKey(), entry.getValue()));

        getPlayerPlaceholderMap(players).entrySet()
                .forEach(entry -> message.addPlaceholderReplace(entry.getKey(), entry.getValue()));

        return message;
    }

    private Map<String, String> getPlayerPlaceholderMap(Player[] players) {

        Map<String, String> toReturn = new HashMap<>();
        toReturn.put("%PLAYER%", players[0].getName());
        for (int i = 1; i < players.length + 1; i++) {
            toReturn.put("PLAYER" + i, players[i - 1].getName());
        }
        return toReturn;
    }

    private void logError(String type, int lineNumber) {
        boolean addLineNumber = lineNumber > 0;
        MessageBuilder builder = MessageBuilder.create(plugin,
                addLineNumber ? "utilCustomActionParseError" : "utilCustomActionParseErrorNoLine")
                .addPlaceholderReplace("%NAME%", actionName)
                .addPlaceholderReplace("%TYPE%", type);
        if (addLineNumber) builder.addPlaceholderReplace("%LINE%", String.valueOf(lineNumber + 1));
        builder.logMessage(Level.WARNING);
    }

    private enum Type {
        REQUIRED_PLAYERS    ("players"),
        CONSOLE_COMMAND     ("cmd"),
        GIVE_CUSTOM_ITEM    ("give"),
        BROADCAST           ("broadcast"),
        TELL                ("tell");

        final String name;

        Type(String fromString) {
            name = fromString;
        }

        static Type getFromString(String stringType) {

            String lowercasedStringType = stringType.toLowerCase();
            for (Type type : Type.values()) {
                if (type.name.equals(lowercasedStringType))
                    return type;
            }
            return null;
        }
    }
}