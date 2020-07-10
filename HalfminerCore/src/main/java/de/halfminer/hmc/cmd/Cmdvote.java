package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.cache.ActionProbabilityContainer;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Message;
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
                    if (!hasVoted.getName().equalsIgnoreCase(voteName)) {
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
                Message.create("cmdVoteVoted", hmc, "Vote")
                        .addPlaceholder("%PLAYER%", hasVoted.getName())
                        .broadcast(true);

                // if the player is currently online, save his ip so that other people with same ip who cannot vote
                // can also bypass the block, also drop vote reward, or increment background reward counter
                if (hasVoted.getBase() instanceof Player) {

                    Player playerHasVoted = (Player) hasVoted.getBase();
                    String address = hasVoted.getIPAddress().replace('.', 'i');
                    coreStorage.incrementInt("vote.ip" + address, 1);

                    boolean receivedReward = giveReward(playerHasVoted);
                    if (!receivedReward) {
                        coreStorage.incrementInt("vote.reward." + playerHasVoted.getUniqueId(), 1);
                        Message.create("cmdVoteRewardCouldNotExecute", hmc, "Vote").send(playerHasVoted);
                    }

                    playerHasVoted.playSound(playerHasVoted.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                } else {
                    // player not online, let him grab the reward later
                    coreStorage.incrementInt("vote.reward." + hasVoted.getUniqueId(), 1);
                }

                // check if threshold has been met to execute notify command
                int totalvotes = coreStorage.incrementInt("totalvotes", 1);
                if (totalvotes == hmc.getConfig().getInt("command.vote.threshold", 2000)) {

                    String command = hmc.getConfig().getString("command.vote.commandToExecute", "");
                    if (command.length() > 0) {
                        server.dispatchCommand(server.getConsoleSender(), command);
                    }
                }

            } else if (isPlayer && args[0].equalsIgnoreCase("getreward")) {

                final String storageKey = "vote.reward." + player.getUniqueId();
                int rewardAmount = coreStorage.getInt(storageKey);

                if (rewardAmount == 0) {
                    Message.create("cmdVoteRewardDeny", hmc, "Vote").send(player);
                    return;
                }

                while (rewardAmount > 0) {
                    if (giveReward(player)) rewardAmount--;
                    else {
                        Message.create("cmdVoteRewardCouldNotExecute", hmc, "Vote").send(player);
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

        Message.create("cmdVoteTop", hmc).send(sender);
        Message.create("cmdVoteMessage", hmc).addPlaceholder("%PLAYER%", playername).send(sender);
        Message.create(" ", hmc).setDirectString().send(sender);

        boolean thresholdPassed = totalVotes < totalVotesThreshold;
        Message.create(thresholdPassed ? "cmdVoteUntil" : "cmdVoteReached", hmc)
                .addPlaceholder("%TOTALVOTES%" + (thresholdPassed ? "" : "%THRESHOLD%"), totalVotes)
                .send(sender);

        if (rewardLeft > 0) Message.create("cmdVoteGrabReward", hmc).send(sender);

        Message.create("lineSeparator").send(sender);
    }

    private boolean giveReward(Player player) {

        List<String> probabilityList = hmc.getConfig().getStringList("command.vote.rewardActions");

        try {
            ActionProbabilityContainer actions = new ActionProbabilityContainer(probabilityList, hmc, coreStorage);
            return actions.getNextAction().runAction(player);
        } catch (CachingException e) {
            Message.create("cmdVoteActionCacheError", hmc)
                    .addPlaceholder("%PLAYER%", player.getName())
                    .addPlaceholder("%REASON%", e.getCleanReason())
                    .log(Level.WARNING);
            return false;
        }
    }
}
