package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.cache.ActionProbabilityContainer;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

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
        this.permission = "hmc.vote";
    }

    @Override
    public void execute() {

        if (args.length > 0) {

            // should be executed by vote listener
            if (args[0].equalsIgnoreCase("voted") && sender.isOp() && args.length == 2) {

                // grab player
                String voteName = args[1];
                HalfminerPlayer hasVoted;
                try {
                    hasVoted = storage.getPlayer(voteName);

                    // only count votes that match the current playername, since our storage call also matches partial and previous names
                    if (!hasVoted.getName().equals(voteName)) {
                        hmc.getLogger().info("Received invalid vote for player '" + voteName + "', argument does not match playername");
                        return;
                    }

                } catch (PlayerNotFoundException e) {
                    hmc.getLogger().info("Received invalid vote for player '" + voteName + "', player not found");
                    return;
                }

                // increment stats, broadcast
                coreStorage.set("vote." + hasVoted.getUniqueId().toString(), Long.MAX_VALUE);
                hasVoted.incrementInt(DataType.VOTES, 1);
                MessageBuilder.create("cmdVoteVoted", hmc, "Vote")
                        .addPlaceholderReplace("%PLAYER%", hasVoted.getName())
                        .broadcastMessage(true);

                // if the player is currently online, save his ip so that other people with same ip who cannot vote
                // can also bypass the block, also drop vote reward, or increment background reward counter
                if (hasVoted.getBase() instanceof Player) {

                    Player playerHasVoted = (Player) hasVoted.getBase();
                    String address = hasVoted.getIPAddress().replace('.', 'i');
                    coreStorage.incrementInt("vote.ip" + address, 1);

                    boolean receivedReward = giveReward(playerHasVoted);
                    if (!receivedReward) {
                        coreStorage.incrementInt("vote.reward." + playerHasVoted.getUniqueId(), 1);
                        MessageBuilder.create("cmdVoteRewardCouldNotExecute", hmc, "Vote").sendMessage(playerHasVoted);
                    }

                    playerHasVoted.playSound(playerHasVoted.getLocation(), Sound.BLOCK_NOTE_PLING, 1.0f, 2.0f);
                } else {
                    // player not online, let him grab the reward later
                    coreStorage.incrementInt("vote.reward." + hasVoted.getUniqueId(), 1);
                }

                // check if threshold has been met to execute notify command
                int totalvotes = coreStorage.incrementInt("totalvotes", 1);
                if (totalvotes == hmc.getConfig().getInt("command.vote.threshold", 2000)) {

                    String command = hmc.getConfig().getString("command.vote.commandToExecute");
                    if (command.length() > 0) {
                        server.dispatchCommand(server.getConsoleSender(), command);
                    }
                }

            } else if (isPlayer && args[0].equalsIgnoreCase("getreward")) {

                final String storageKey = "vote.reward." + player.getUniqueId();
                int rewardAmount = coreStorage.getInt(storageKey);

                if (rewardAmount == 0) {
                    MessageBuilder.create("cmdVoteRewardDeny", hmc, "Vote").sendMessage(player);
                    return;
                }

                while (rewardAmount > 0) {
                    if (giveReward(player)) rewardAmount--;
                    else {
                        MessageBuilder.create("cmdVoteRewardCouldNotExecute", hmc, "Vote").sendMessage(player);
                        break;
                    }
                }

                coreStorage.set(storageKey, rewardAmount);
            } else showMessage();

        } else showMessage();
    }

    private void showMessage() {

        String playername = "";
        int rewardLeft = 0;

        if (isPlayer) {
            playername = player.getName();
            rewardLeft = coreStorage.getInt("vote.reward." + player.getUniqueId());
        }

        int totalVotes = coreStorage.getInt("totalvotes");
        int totalVotesThreshold = hmc.getConfig().getInt("command.vote.threshold", 2000);

        MessageBuilder.create("cmdVoteTop", hmc).sendMessage(sender);
        MessageBuilder.create("cmdVoteMessage", hmc).addPlaceholderReplace("%PLAYER%", playername).sendMessage(sender);
        MessageBuilder.create(" ", hmc).setDirectString().sendMessage(sender);

        boolean thresholdPassed = totalVotes < totalVotesThreshold;
        MessageBuilder.create(thresholdPassed ? "cmdVoteUntil" : "cmdVoteReached", hmc)
                .addPlaceholderReplace("%TOTALVOTES%" + (thresholdPassed ? "" : "%THRESHOLD%"), totalVotes)
                .sendMessage(sender);

        if (rewardLeft > 0) MessageBuilder.create("cmdVoteGrabReward", hmc).sendMessage(sender);

        MessageBuilder.create("lineSeparator").sendMessage(sender);
    }

    private boolean giveReward(Player player) {

        List<String> probabilityList = hmc.getConfig().getStringList("command.vote.rewardActions");

        try {
            ActionProbabilityContainer actions = new ActionProbabilityContainer(probabilityList, hmc, coreStorage);
            return actions.getNextAction().runAction(player);
        } catch (CachingException e) {
            MessageBuilder.create("cmdVoteActionCacheError", hmc)
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .addPlaceholderReplace("%REASON%", e.getCleanReason())
                    .logMessage(Level.WARNING);
            return false;
        }
    }
}
