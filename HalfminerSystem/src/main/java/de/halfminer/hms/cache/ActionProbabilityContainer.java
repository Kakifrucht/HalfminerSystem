package de.halfminer.hms.cache;

import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.util.FormattingException;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.Utils;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

/**
 * Container that takes a string list containing a number on the left side of a ':' and the name of an action on
 * the right side. The higher the number, the higher the probability of returning that action the next call of
 * {@link #getNextAction()}. Invalid lines will be skipped, while logging the issue.
 */
@SuppressWarnings("SameParameterValue")
public class ActionProbabilityContainer {

    private final Plugin plugin;
    private final List<Pair<Integer, CustomAction>> actionList;

    private int probabilityTotal = 0;

    public ActionProbabilityContainer(List<String> probabilityList, Plugin plugin,
                                      CacheHolder cacheHolder) throws CachingException {
        this.plugin = plugin;

        actionList = new ArrayList<>();
        for (int lineNumber = 0; lineNumber < probabilityList.size(); lineNumber++) {

            String actionToParse = probabilityList.get(lineNumber);
            Pair<String, String> keyValuePair;
            try {
                keyValuePair = Utils.getKeyValuePair(actionToParse);
            } catch (FormattingException e) {
                logError("FORMAT", lineNumber);
                continue;
            }

            int probability;
            try {
                probability = Integer.parseInt(keyValuePair.getLeft());
                if (probability < 1) {
                    logError("NUMBER", lineNumber);
                    continue;
                }
            } catch (NumberFormatException e) {
                logError("NUMBER", lineNumber);
                continue;
            }

            probabilityTotal += probability;

            try {
                CustomAction action = new CustomAction(keyValuePair.getRight(), cacheHolder);
                actionList.add(new Pair<>(probabilityTotal, action));
            } catch (CachingException e) {
                if (e.getReason().equals(CachingException.Reason.CHAPTER_NOT_FOUND))
                    logError("ACTIONNAME", lineNumber);
                else throw e;
            }
        }

        if (actionList.size() == 0) {
            probabilityTotal = 1;
            actionList.add(new Pair<>(1, new CustomAction("nothing", cacheHolder)));
            Message.create("cacheActionProbabilityContainerNoActions").log(Level.WARNING);
        }
    }

    public CustomAction getNextAction() {

        int random = new Random().nextInt(probabilityTotal);
        for (Pair<Integer, CustomAction> pair : actionList) {
            if (random < pair.getLeft())
                return pair.getRight();
        }
        throw new RuntimeException("No action was selected");
    }

    private void logError(String type, int lineNumber) {
        Message.create("cacheActionProbabilityContainerError", plugin)
                .addPlaceholder("%TYPE%", type)
                .addPlaceholder("%LINE%", lineNumber + 1)
                .log(Level.WARNING);
    }
}
