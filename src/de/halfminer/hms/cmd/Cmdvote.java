package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.ActionProbabilityContainer;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.logging.Level;

/**
 * - Shows vote links (custom per player) and current votecount
 * - Execute custom actions when vote is received (configure Votifier to "/vote voted %PLAYER%")
 * - Execute command if certain votecount has been reached (event notifier for instance)
 * - If offline or inventory full, stores reward for retrieval later (/vote getreward)
 * - Counts votes for /stats
 * - Unblocks access to /home
 * - Will also unblock other users with same ip
 */
@SuppressWarnings("unused")
public class Cmdvote extends HalfminerCommand {

    public Cmdvote() {
        this.permission = "hms.vote";
    }

    @Override
    public void execute() {

        if (args.length > 0) {

            // should be executed by vote listener
            if (args[0].equalsIgnoreCase("voted") && sender.isOp() && args.length == 2) {

                // grab player
                HalfminerPlayer hasVoted;
                try {
                    hasVoted = storage.getPlayer(args[1]);
                } catch (PlayerNotFoundException e) {
                    return;
                }

                // increment stats, broadcast
                storage.set("vote." + hasVoted.getUniqueId().toString(), Long.MAX_VALUE);
                hasVoted.incrementInt(DataType.VOTES, 1);
                MessageBuilder.create(hms, "cmdVoteVoted", "Vote")
                        .addPlaceholderReplace("%PLAYER%", hasVoted.getName())
                        .broadcastMessage(true);

                // if the player is currently online, save his ip so that other people with same ip who cannot vote
                // can also bypass the block, also drop vote reward, or increment background reward counter
                if (hasVoted.getBase() instanceof Player) {

                    Player playerHasVoted = (Player) hasVoted.getBase();
                    String address = playerHasVoted.getAddress().getAddress().toString().replace('.', 'i').substring(1);
                    storage.incrementInt("vote.ip" + address, 1);

                    boolean receivedReward = giveReward(playerHasVoted);
                    if (!receivedReward) {
                        storage.incrementInt("vote.reward." + playerHasVoted.getUniqueId(), 1);
                        MessageBuilder.create(hms, "cmdVoteRewardInvFull", "Vote").sendMessage(playerHasVoted);
                    }

                    playerHasVoted.playSound(playerHasVoted.getLocation(), Sound.BLOCK_NOTE_PLING, 1.0f, 2.0f);
                } else {
                    // player not online, let him grab the reward later
                    storage.incrementInt("vote.reward." + hasVoted.getUniqueId(), 1);
                }

                // check if threshold has been met to execute notify command
                int totalvotes = storage.incrementInt("totalvotes", 1);
                if (totalvotes == hms.getConfig().getInt("command.vote.threshold", 2000)) {

                    String command = hms.getConfig().getString("command.vote.commandToExecute");
                    if (command.length() > 0) {
                        server.dispatchCommand(server.getConsoleSender(), command);
                    }
                }

            } else if (isPlayer && args[0].equalsIgnoreCase("getreward")) {

                final String storageKey = "vote.reward." + player.getUniqueId();
                final int rewardAmount = storage.getInt(storageKey);

                if (rewardAmount == 0) {
                    MessageBuilder.create(hms, "cmdVoteRewardDeny", "Vote").sendMessage(player);
                    return;
                }

                // set to 0 immediately to not allow the execution twice
                storage.set(storageKey, null);

                // execute one reward action per second
                new BukkitRunnable() {

                    private int rewardAmountTask = rewardAmount;

                    @Override
                    public void run() {
                        if (rewardAmountTask > 0 && giveReward(player)) rewardAmountTask--;
                        else {
                            // if action could not be executed, send message
                            if (rewardAmountTask > 0) {
                                MessageBuilder.create(hms, "cmdVoteRewardCouldNotExecute", "Vote")
                                        .sendMessage(player);
                                storage.set(storageKey, rewardAmountTask);
                            }

                            this.cancel();
                        }
                    }
                }.runTaskTimer(hms, 0L, 20L);

            } else showMessage();

        } else showMessage();
    }

    private void showMessage() {

        String playername = "";
        int rewardLeft = 0;

        if (isPlayer) {
            playername = player.getName();
            rewardLeft = storage.getInt("vote.reward." + player.getUniqueId());
        }

        int totalVotes = storage.getInt("totalvotes");
        int totalVotesThreshold = hms.getConfig().getInt("command.vote.threshold", 2000);

        MessageBuilder.create(hms, "cmdVoteTop").sendMessage(sender);
        MessageBuilder.create(hms, "cmdVoteMessage").addPlaceholderReplace("%PLAYER%", playername).sendMessage(sender);
        MessageBuilder.create(hms, " ").setMode(MessageBuilder.Mode.DIRECT_STRING).sendMessage(sender);

        boolean thresholdPassed = totalVotes < totalVotesThreshold;
        MessageBuilder.create(hms, thresholdPassed ? "cmdVoteUntil" : "cmdVoteReached")
                .addPlaceholderReplace(thresholdPassed ?
                        "%TOTALVOTES%" : "%TOTALVOTESTHRESHOLD%", String.valueOf(totalVotes))
                .sendMessage(sender);

        if (rewardLeft > 0) MessageBuilder.create(hms, "cmdVoteGrabReward").sendMessage(sender);

        MessageBuilder.create(hms, "lineSeparator").sendMessage(sender);
    }

    private boolean giveReward(Player player) {

        List<String> probabilityList = hms.getConfig().getStringList("command.vote.rewardActions");
        if (probabilityList.size() == 0)
            return false;

        try {
            ActionProbabilityContainer actions = new ActionProbabilityContainer(probabilityList, hms, storage);
            return actions.getNextAction().runAction(player);
        } catch (CachingException e) {
            MessageBuilder.create(hms, "cmdVoteActionCacheError")
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .addPlaceholderReplace("%REASON%", e.getCleanReason())
                    .logMessage(Level.WARNING);
            return false;
        }
    }
}
