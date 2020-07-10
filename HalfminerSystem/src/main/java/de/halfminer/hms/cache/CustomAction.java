package de.halfminer.hms.cache;

import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.util.FormattingException;
import de.halfminer.hms.cache.exceptions.ItemCacheException;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hms.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
    private final Plugin plugin;
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
        this.itemCache = new CustomitemCache(holder.getCache("customitems.txt"));
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
                parsedActionList = null;
                return;
            }

            Type type = Type.getFromString(keyValuePair.getLeft());
            if (type != null) {
                if (type.equals(Type.REQUIRED_PLAYERS)) {
                    try {
                        playersRequired = Integer.parseInt(keyValuePair.getRight());
                        playersRequired = Math.max(playersRequired, 1);
                    } catch (NumberFormatException e) {
                        logError("PLAYERSREQUIRED", lineNumber);
                        parsedActionList = null;
                        return;
                    }
                } else parsedActionList.add(new Pair<>(type, keyValuePair.getRight()));
            } else {
                logError("ACTIONTYPE", lineNumber);
                parsedActionList = null;
                return;
            }
        }
    }

    public boolean runAction(Player... players) {

        // reparse if cache was modified
        if (actionCache.wasModifiedSince(lastCached)) {
            try {
                parseAction();
                lastCached = System.currentTimeMillis();
            } catch (CachingException ignored) {
            }
        }

        // support empty action
        if (parsedActionList == null) return true;

        if (players.length < playersRequired) {
            logError("NOTENOUGHPLAYERS", -1);
            return false;
        }

        placeholders.put("%PLAYER%", players[0].getName());
        for (int i = 1; i < players.length + 1; i++) {
            placeholders.put("PLAYER" + i, players[i - 1].getName());
        }

        for (Pair<Type, String> action : parsedActionList) {

            Message parsedMessage = replaceWithPlaceholders(action.getRight());

            switch (action.getLeft()) {
                case CONSOLE_COMMAND:
                    boolean success = plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(),
                            parsedMessage.returnMessage());
                    if (!success) {
                        Message.create("cacheCustomActionCommandNotFound")
                                .addPlaceholder("%COMMAND%", parsedMessage.returnMessage())
                                .log(Level.WARNING);
                        return false;
                    }
                    break;
                case GIVE_CUSTOM_ITEM:
                    try {
                        StringArgumentSeparator separator = new StringArgumentSeparator(parsedMessage.returnMessage());
                        itemCache.giveItem(separator.getArgument(0), players[0],
                                separator.getArgumentIntMinimum(1, 1), placeholders);
                    } catch (ItemCacheException e) {

                        if (!e.getReason().equals(ItemCacheException.Reason.INVENTORY_FULL)) {
                            Message.create("cacheCustomActionGiveItemError")
                                    .addPlaceholder("%ITEM%", parsedMessage.returnMessage())
                                    .addPlaceholder("%REASON%", e.getCleanReason())
                                    .log(Level.WARNING);
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
                    if (action.getLeft() == Type.BROADCAST) parsedMessage.broadcast(true);
                    else parsedMessage.send(players[0]);
            }
        }
        placeholders.clear();
        return true;
    }

    public void addPlaceholderForNextRun(String placeholder, String replaceWith) {
        placeholders.put(placeholder, replaceWith);
    }

    private Message replaceWithPlaceholders(String toReplace) {

        Message message = Message.create(toReplace).setDirectString();
        placeholders.forEach(message::addPlaceholder);
        return message;
    }

    private void logError(String type, int lineNumber) {
        boolean addLineNumber = lineNumber >= 0;
        Message builder = Message.create(
                addLineNumber ? "cacheCustomActionParseError" : "cacheCustomActionParseErrorNoLine")
                .addPlaceholder("%NAME%", actionName)
                .addPlaceholder("%TYPE%", type);
        if (addLineNumber) builder.addPlaceholder("%LINE%", lineNumber + 1);
        builder.log(Level.WARNING);
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
