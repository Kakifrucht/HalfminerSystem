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
    private final CustomtextCache actionCache;

    private long lastCached;
    private List<Pair<Type, String>> parsedActionList;
    private int playersRequired = 1;

    private final Map<String, String> placeholders = new HashMap<>();

    /**
     * Create a new action. If the action name is "nothing", the action won't do anything on run.
     *
     * @param action String naming the action
     * @param holder class holding necessary caches (customactions.txt and customitems.txt)
     * @throws CachingException if the action could not be found or the caches could not be
     *                          initialized due to {@link java.io.IOException}
     */
    public CustomAction(String action, CacheHolder holder) throws CachingException {

        this.actionName = action.toLowerCase();
        this.plugin = holder.getPlugin();
        this.itemCache = new CustomitemCache(plugin, holder.getCache("customitems.txt"));
        this.actionCache = holder.getCache("customactions.txt");
        this.lastCached = System.currentTimeMillis();

        parseAction();
    }

    private void parseAction() throws CachingException {

        if (actionName.equalsIgnoreCase("nothing")) {
            this.parsedActionList = null;
            return;
        } else this.parsedActionList = new ArrayList<>();

        List<String> actionUnparsed = actionCache.getChapter(actionName);

        for (int lineNumber = 0; lineNumber < actionUnparsed.size(); lineNumber++) {

            String line = actionUnparsed.get(lineNumber);

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

    public boolean runAction(Player... players) {

        // support empty action
        if (parsedActionList == null) return true;

        if (players.length < playersRequired) {
            logError("NOTENOUGHPLAYERS", 0);
            return false;
        }

        // reparse if cache was modified
        if (actionCache.wasModifiedSince(lastCached)) {
            try {
                parseAction();
                lastCached = System.currentTimeMillis();
            } catch (CachingException ignored) {
            }
        }

        placeholders.put("%PLAYER%", players[0].getName());
        for (int i = 1; i < players.length + 1; i++) {
            placeholders.put("PLAYER" + i, players[i - 1].getName());
        }

        for (Pair<Type, String> action : parsedActionList) {

            MessageBuilder parsedMessage = replaceWithPlaceholders(action.getRight());

            switch (action.getLeft()) {
                case CONSOLE_COMMAND:
                    boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            parsedMessage.returnMessage());
                    if (!success) {
                        MessageBuilder.create(plugin, "utilCustomActionCommandNotFound")
                                .addPlaceholderReplace("%COMMAND%", parsedMessage.returnMessage())
                                .logMessage(Level.WARNING);
                        return false;
                    }
                    break;
                case GIVE_CUSTOM_ITEM:
                    try {
                        StringArgumentSeparator separator = new StringArgumentSeparator(parsedMessage.returnMessage());
                        itemCache.giveItem(separator.getArgument(0), players[0],
                                separator.getArgumentIntMinimum(1, 1), placeholders);
                    } catch (GiveItemException e) {

                        if (!e.getReason().equals(GiveItemException.Reason.INVENTORY_FULL)) {
                            MessageBuilder.create(plugin, "utilCustomActionGiveItemError")
                                    .addPlaceholderReplace("%ITEM%", parsedMessage.returnMessage())
                                    .addPlaceholderReplace("%REASON%", e.getCleanReason())
                                    .logMessage(Level.WARNING);
                        }
                        // quit execution of action on fail
                        placeholders.clear();
                        return false;
                    }
                    break;
                case HAS_ROOM:
                    StringArgumentSeparator separator = new StringArgumentSeparator(parsedMessage.returnMessage());
                    int amountCheck = separator.getArgumentIntMinimum(0, 1);
                    if (separator.meetsLength(2)) {
                        int stackSize = separator.getArgumentIntMinimum(1, 1);
                        boolean hasRemainder = (amountCheck % stackSize) != 0;
                        amountCheck /= stackSize;
                        if (hasRemainder) amountCheck++;
                    }
                    if (!Utils.hasRoom(players[0], amountCheck)) {
                        placeholders.clear();
                        return false;
                    }
                    break;
                case BROADCAST:
                case TELL:
                    if (action.getLeft() == Type.BROADCAST) parsedMessage.broadcastMessage(true);
                    else parsedMessage.sendMessage(players[0]);
            }
        }
        placeholders.clear();
        return true;
    }

    public void addPlaceholderForNextRun(String placeholder, String replaceWith) {
        placeholders.put(placeholder, replaceWith);
    }

    private MessageBuilder replaceWithPlaceholders(String toReplace) {

        MessageBuilder message = MessageBuilder.create(plugin, toReplace)
                .setDirectString();

        placeholders.entrySet()
                .forEach(entry -> message.addPlaceholderReplace(entry.getKey(), entry.getValue()));

        return message;
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
        HAS_ROOM            ("hasroom"),
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
