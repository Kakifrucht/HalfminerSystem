package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * - Shows vote links (custom per player) and current votecount
 * - Execute custom command when vote is received (configure Votifier to "/vote voted %PLAYER%")
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
    public void run(CommandSender sender, String label, String[] args) {

        if (args.length > 0) {

            //will be executed by vote listener
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
                server.broadcast(Language.getMessagePlaceholders("cmdVoteVoted", true, "%PREFIX%",
                        "Vote", "%PLAYER%", hasVoted.getName()), "hms.default");

                // if the player is currently online, save his ip so that other people with same ip who cannot vote
                // can also bypass the block, also drop vote reward, or increment background reward counter
                if (hasVoted.getBase() instanceof Player) {
                    Player playerHasVoted = (Player) hasVoted.getBase();
                    playerHasVoted.playSound(playerHasVoted.getLocation(), Sound.BLOCK_NOTE_PLING, 1.0f, 2.0f);
                    String address = playerHasVoted.getAddress().getAddress().toString().replace('.', 'i').substring(1);
                    storage.incrementInt("vote.ip" + address, 1);
                    if (!dropCase(playerHasVoted)) {
                        storage.incrementInt("vote.reward." + playerHasVoted.getUniqueId(), 1);
                        playerHasVoted.sendMessage(Language.getMessagePlaceholders("cmdVoteRewardInvFull", true, "%PREFIX%", "Vote"));
                    }
                } else {
                    // player not online, let him grab the reward later
                    storage.incrementInt("vote.reward." + hasVoted.getUniqueId(), 1);
                }

                // check if threshold has been met to message admin
                int totalvotes = storage.incrementInt("totalvotes", 1);
                if (totalvotes == hms.getConfig().getInt("command.vote.threshold", 2000)) {

                    String command = hms.getConfig().getString("command.vote.commandToExecute");
                    if (command.length() > 0) {
                        server.dispatchCommand(server.getConsoleSender(), command);
                    }
                }

            } else if (args[0].equalsIgnoreCase("getreward") && sender instanceof Player) {

                Player player = (Player) sender;
                int rewardAmount = storage.getInt("vote.reward." + player.getUniqueId());

                if (rewardAmount == 0) {
                    player.sendMessage(Language.getMessagePlaceholders("cmdVoteRewardDeny", true, "%PREFIX%", "Vote"));
                    return;
                }

                while (rewardAmount > 0 && dropCase(player)) rewardAmount--;

                //Reward could not be paid due to full inventory, send message
                if (rewardAmount > 0) {
                    player.sendMessage(Language.getMessagePlaceholders("cmdVoteRewardInvFull",
                            true, "%PREFIX%", "Vote"));
                }
                storage.set("vote.reward." + player.getUniqueId(), rewardAmount);

            } else showMessage(sender);

        } else showMessage(sender);
    }

    private void showMessage(CommandSender sender) {

        String playername = "";
        int rewardLeft = 0;

        if (sender instanceof Player) {
            playername = sender.getName();
            rewardLeft = storage.getInt("vote.reward." + ((Player) sender).getUniqueId());
        }

        int totalVotes = storage.getInt("totalvotes");
        int totalVotesThreshold = hms.getConfig().getInt("command.vote.threshold", 2000);

        String message = Language.getMessage("cmdVoteTop") + "\n"
                + Language.getMessagePlaceholders("cmdVoteMessage", false, "%PLAYER%", playername) + "\n \n";

        if (totalVotes < totalVotesThreshold) {
            message += Language.getMessagePlaceholders("cmdVoteUntil", false
                    , "%TOTALVOTES%", String.valueOf(totalVotes)) + "\n";
        } else {
            message += Language.getMessagePlaceholders("cmdVoteReached", false,
                    "%TOTALVOTESTHRESHOLD%", String.valueOf(totalVotes)) + "\n";
        }

        if (rewardLeft > 0) {
            message += Language.getMessage("cmdVoteGrabReward") + "\n";
        }

        message += Language.getMessage("lineSeparator");

        sender.sendMessage(message);
    }

    private boolean dropCase(Player player) {

        boolean hasRoom = false;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null) {
                hasRoom = true;
                break;
            }
        }

        if (!hasRoom) return false;

        String command = hms.getConfig().getString("command.vote.voteRewardCommand");
        command = Language.placeholderReplace(command, "%PLAYER%", player.getName());
        server.dispatchCommand(server.getConsoleSender(), command);
        return true;
    }
}
