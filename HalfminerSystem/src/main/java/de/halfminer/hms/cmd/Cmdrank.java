package de.halfminer.hms.cmd;

import de.halfminer.hms.cmd.abs.HalfminerPersistenceCommand;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.CustomAction;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * - Give out ranks to players
 * - Executes a custom action to give out rewards and run custom commands
 *   - If player is offline waits until he is back online to execute action
 *   - If action fails executes custom fallback action
 *   - Add custom parameters that will be multiplied by a custom amount per rank (see config)
 *     - Possibility to deduct reward multipliers for previous ranks
 * - Prevents giving out same or lower rank
 * - Instead of defining upgrade rank on command execution can define number of ranks that player will be upranked
 */
@SuppressWarnings("unused")
public class Cmdrank extends HalfminerPersistenceCommand {

    private final List<Pair<String, Integer>> rankNameAndMultiplierPairs = new ArrayList<>();

    private int upgradeAmount = Integer.MIN_VALUE;
    private String rankToGiveName;
    private int rankToGiveMultiplier;

    public Cmdrank() {
        this.permission = "hms.rank";
    }

    @Override
    protected void execute() {

        if (args.length < 2) {
            MessageBuilder.create(hms, "cmdRankUsage", "Rank").sendMessage(sender);
            return;
        }

        Player playerToReward = server.getPlayerExact(args[0]);
        UUID uuidToReward;
        if (playerToReward == null) {
            try {
                uuidToReward = storage.getPlayer(args[0]).getUniqueId();
            } catch (PlayerNotFoundException e) {
                e.sendNotFoundMessage(sender, "Rank");
                return;
            }
        } else uuidToReward = playerToReward.getUniqueId();

        for (String level : hms.getConfig().getStringList("command.rank.rankNamesAndMultipliers")) {

            StringArgumentSeparator current = new StringArgumentSeparator(level, ',');

            if (!current.meetsLength(2)) {
                sendInvalidRankConfig(level);
                return;
            }

            String currentRank = current.getArgument(0);
            int multiplier = current.getArgumentInt(1);
            if (multiplier < 1) {
                sendInvalidRankConfig(level);
                return;
            }
            if (currentRank.equalsIgnoreCase(args[1])) {
                rankToGiveName = currentRank;
                rankToGiveMultiplier = multiplier;
            }
            rankNameAndMultiplierPairs.add(new Pair<>(currentRank, multiplier));
        }

        if (rankToGiveName == null) {
            try {
                upgradeAmount = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {}

            if (upgradeAmount > rankNameAndMultiplierPairs.size() || upgradeAmount < 1) {
                MessageBuilder.create(hms, "cmdRankInvalidRankCommand", "Rank").sendMessage(sender);
                return;
            }
        }

        if (playerToReward != null) {
            execute(playerToReward);
        } else {
            MessageBuilder.create(hms, "cmdRankNotOnline", "Rank")
                    .addPlaceholderReplace("%PLAYER%", args[0])
                    .sendMessage(sender);
            setPersistent(PersistenceMode.EVENT_PLAYER_JOIN, uuidToReward);
        }
    }

    @Override
    public boolean execute(PlayerEvent e) {
        execute(e.getPlayer());
        return true;
    }

    private void execute(Player player) {

        if (player.isOp()) {
            MessageBuilder send = MessageBuilder.create(hms, "cmdRankPlayerIsOp", "Rank")
                    .addPlaceholderReplace("%PLAYER%", player.getName());
            sendAndLogMessageBuilder(send);
            return;
        }

        int playerLevel = 0;
        while (playerLevel < 6 && player.hasPermission("hms.level." + (playerLevel + 1))) {
            playerLevel++;
        }

        if (rankToGiveName == null) {
            int getFromList = playerLevel + upgradeAmount - 1;
            if (getFromList >= rankNameAndMultiplierPairs.size()) {
                MessageBuilder send = MessageBuilder.create(hms, "cmdRankInvalidUpgradeParam", "Rank")
                        .addPlaceholderReplace("%PLAYER%", player.getName())
                        .addPlaceholderReplace("%UPGRADEAMOUNT%", String.valueOf(upgradeAmount));
                sendAndLogMessageBuilder(send);
                return;
            }
            Pair<String, Integer> rankPair = rankNameAndMultiplierPairs.get(getFromList);
            rankToGiveName = rankPair.getLeft();
            rankToGiveMultiplier = rankPair.getRight();
        }

        List<Integer> baseAmounts = hms.getConfig().getIntegerList("command.rank.baseAmountValues");
        List<Integer> multipliedAmounts = new ArrayList<>();
        for (Integer base : baseAmounts) {
            multipliedAmounts.add(base * rankToGiveMultiplier);
        }

        if (playerLevel > 0) {
            // check if new level is lower/same as old one
            int multiplierOfPreviousRank = rankNameAndMultiplierPairs.get(playerLevel - 1).getRight();
            if (multiplierOfPreviousRank >= rankToGiveMultiplier) {
                MessageBuilder send = MessageBuilder.create(hms, "cmdRankNewLevelSameOrLower", "Rank")
                        .addPlaceholderReplace("%PLAYER%", player.getName())
                        .addPlaceholderReplace("%NEWRANK%", rankToGiveName);
                sendAndLogMessageBuilder(send);
                return;
            }

            if (hms.getConfig().getBoolean("command.rank.deductPreviousRanks")) {
                for (int i = 0; i < multipliedAmounts.size(); i++) {
                    int current = multipliedAmounts.get(i);
                    int alreadyGiven = baseAmounts.get(i) * multiplierOfPreviousRank;
                    multipliedAmounts.set(i, current - alreadyGiven);
                }
            }
        }

        String actionName = hms.getConfig().getString("command.rank.actionToExecute");
        boolean actionHasFailed = true;
        try {
            CustomAction action = new CustomAction(actionName, storage);
            addPlaceholdersToAction(action, multipliedAmounts);
            actionHasFailed = !action.runAction(player);
        } catch (CachingException e1) {
            logActionNotFound(player, actionName);
        }

        if (actionHasFailed) {
            String actionOnFail = hms.getConfig().getString("command.rank.actionToExecuteOnFail");
            try {
                if (actionOnFail.length() > 0) {
                    CustomAction failAction = new CustomAction(actionOnFail, storage);
                    addPlaceholdersToAction(failAction, multipliedAmounts);
                    failAction.runAction(player);
                }
            } catch (CachingException e1) {
                logActionNotFound(player, actionOnFail);
            }
        }
    }

    @Override
    public void onDisable() {

        String commandOnDisable = hms.getConfig().getString("command.rank.commandToExecuteOnDisable");
        if (commandOnDisable.length() > 0) {
            String placeholderReplaced = MessageBuilder.create(hms, commandOnDisable)
                    .setDirectString()
                    .addPlaceholderReplace("%PLAYER%", args[0])
                    .addPlaceholderReplace("%RANK%", rankToGiveName)
                    .returnMessage();

            server.dispatchCommand(server.getConsoleSender(), placeholderReplaced);
        }
        MessageBuilder.create(hms, "cmdRankPersistenceDisable")
                .addPlaceholderReplace("%PLAYER%", args[0])
                .addPlaceholderReplace("%RANK%", rankToGiveName)
                .logMessage(Level.WARNING);
    }

    private void sendInvalidRankConfig(String level) {
        MessageBuilder.create(hms, "cmdRankInvalidRankConfig", "Rank")
                .addPlaceholderReplace("%INVALIDINPUT%", level)
                .sendMessage(sender);
    }

    private void addPlaceholdersToAction(CustomAction action, List<Integer> multipliedAmounts) {
        action.addPlaceholderForNextRun("%PARAM1%", rankToGiveName);
        for (int i = 0; i < multipliedAmounts.size(); i++) {
            action.addPlaceholderForNextRun("%PARAM" + (i + 2) + "%", String.valueOf(multipliedAmounts.get(i)));
        }
    }

    private void logActionNotFound(Player toReward, String actionName) {
        MessageBuilder notify = MessageBuilder.create(hms, "cmdRankActionNotFound", "Rank")
                .addPlaceholderReplace("%PLAYER%", toReward.getName())
                .addPlaceholderReplace("%ACTIONNAME%", actionName);
        sendAndLogMessageBuilder(notify);
    }

    private void sendAndLogMessageBuilder(MessageBuilder toSendAndLog) {
        toSendAndLog.logMessage(Level.SEVERE);
        CommandSender originalSender = getOriginalSender();
        if (originalSender != null) toSendAndLog.sendMessage(originalSender);
    }
}
