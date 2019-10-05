package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerPersistenceCommand;
import de.halfminer.hms.cache.CustomAction;
import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

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
public class Cmdrank extends HalfminerPersistenceCommand implements Disableable {

    private final List<Pair<String, Integer>> rankNameAndMultiplierPairs = new ArrayList<>();

    private int upgradeAmount = Integer.MIN_VALUE;
    private String rankToGiveName;
    private int rankToGiveMultiplier;


    @Override
    protected void execute() {

        if (args.length < 2) {
            MessageBuilder.create("cmdRankUsage", hmc, "Rank").sendMessage(sender);
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

        for (String level : hmc.getConfig().getStringList("command.rank.rankNamesAndMultipliers")) {

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
                MessageBuilder.create("cmdRankInvalidRankCommand", hmc, "Rank").sendMessage(sender);
                return;
            }
        }

        if (playerToReward != null) {
            execute(playerToReward);
        } else {
            MessageBuilder.create("cmdRankNotOnline", hmc, "Rank")
                    .addPlaceholder("%PLAYER%", args[0])
                    .sendMessage(sender);
            setPersistent(uuidToReward);
        }
    }

    @EventHandler
    public void execute(PlayerJoinEvent e) {
        if (isPersistenceOwner(e.getPlayer())) {
            execute(e.getPlayer());
            unregisterClass();
        }
    }

    private void execute(Player player) {

        if (player.isOp()) {
            MessageBuilder send = MessageBuilder.create("cmdRankPlayerIsOp", hmc, "Rank")
                    .addPlaceholder("%PLAYER%", player.getName());
            sendAndLogMessageBuilder(send);
            return;
        }

        int playerLevel = storage.getPlayer(player).getLevel();

        if (rankToGiveName == null) {
            int getFromList = playerLevel + upgradeAmount - 1;
            if (getFromList >= rankNameAndMultiplierPairs.size()) {
                MessageBuilder send = MessageBuilder.create("cmdRankInvalidUpgradeParam", hmc, "Rank")
                        .addPlaceholder("%PLAYER%", player.getName())
                        .addPlaceholder("%UPGRADEAMOUNT%", upgradeAmount);
                sendAndLogMessageBuilder(send);
                return;
            }
            Pair<String, Integer> rankPair = rankNameAndMultiplierPairs.get(getFromList);
            rankToGiveName = rankPair.getLeft();
            rankToGiveMultiplier = rankPair.getRight();
        }

        List<Integer> baseAmounts = hmc.getConfig().getIntegerList("command.rank.baseAmountValues");
        List<Integer> multipliedAmounts = new ArrayList<>();
        for (Integer base : baseAmounts) {
            multipliedAmounts.add(base * rankToGiveMultiplier);
        }

        if (playerLevel > 0) {
            // check if new level is lower/same as old one
            int multiplierOfPreviousRank = rankNameAndMultiplierPairs.get(playerLevel - 1).getRight();
            if (multiplierOfPreviousRank >= rankToGiveMultiplier) {
                MessageBuilder send = MessageBuilder.create("cmdRankNewLevelSameOrLower", hmc, "Rank")
                        .addPlaceholder("%PLAYER%", player.getName())
                        .addPlaceholder("%NEWRANK%", rankToGiveName);
                sendAndLogMessageBuilder(send);
                return;
            }

            if (hmc.getConfig().getBoolean("command.rank.deductPreviousRanks")) {
                for (int i = 0; i < multipliedAmounts.size(); i++) {
                    int current = multipliedAmounts.get(i);
                    int alreadyGiven = baseAmounts.get(i) * multiplierOfPreviousRank;
                    multipliedAmounts.set(i, current - alreadyGiven);
                }
            }
        }

        String actionName = hmc.getConfig().getString("command.rank.actionToExecute");
        boolean actionHasFailed = true;
        try {
            CustomAction action = new CustomAction(actionName, coreStorage);
            addPlaceholdersToAction(action, multipliedAmounts);
            actionHasFailed = !action.runAction(player);
        } catch (CachingException e1) {
            logActionNotFound(player, actionName);
        }

        if (actionHasFailed) {
            String actionOnFail = hmc.getConfig().getString("command.rank.actionToExecuteOnFail");
            try {
                if (actionOnFail.length() > 0) {
                    CustomAction failAction = new CustomAction(actionOnFail, coreStorage);
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

        String commandOnDisable = hmc.getConfig().getString("command.rank.commandToExecuteOnDisable");
        if (commandOnDisable.length() > 0) {
            String placeholderReplaced = MessageBuilder.create(commandOnDisable, hmc)
                    .setDirectString()
                    .addPlaceholder("%PLAYER%", args[0])
                    .addPlaceholder("%ARG%", args[1])
                    .returnMessage();

            server.dispatchCommand(server.getConsoleSender(), placeholderReplaced);
        }
        MessageBuilder.create("cmdRankPersistenceDisable", hmc)
                .addPlaceholder("%PLAYER%", args[0])
                .addPlaceholder("%ARG%", args[1])
                .logMessage(Level.WARNING);
    }

    private void sendInvalidRankConfig(String level) {
        MessageBuilder.create("cmdRankInvalidRankConfig", hmc, "Rank")
                .addPlaceholder("%INVALIDINPUT%", level)
                .sendMessage(sender);
    }

    private void addPlaceholdersToAction(CustomAction action, List<Integer> multipliedAmounts) {
        action.addPlaceholderForNextRun("%PARAM1%", rankToGiveName);
        for (int i = 0; i < multipliedAmounts.size(); i++) {
            action.addPlaceholderForNextRun("%PARAM" + (i + 2) + "%", String.valueOf(multipliedAmounts.get(i)));
        }
    }

    private void logActionNotFound(Player toReward, String actionName) {
        MessageBuilder notify = MessageBuilder.create("cmdRankActionNotFound", hmc, "Rank")
                .addPlaceholder("%PLAYER%", toReward.getName())
                .addPlaceholder("%ACTIONNAME%", actionName);
        sendAndLogMessageBuilder(notify);
    }

    private void sendAndLogMessageBuilder(MessageBuilder toSendAndLog) {
        toSendAndLog.logMessage(Level.WARNING);
        CommandSender originalSender = getOriginalSender();
        if (originalSender != null) toSendAndLog.sendMessage(originalSender);
    }
}
