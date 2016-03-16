package de.halfminer.hms.cmd;

import de.halfminer.hms.exception.PlayerNotFoundException;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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

                //grab player
                OfflinePlayer hasVoted;
                try {
                    hasVoted = hms.getServer().getOfflinePlayer(storage.getUUID(args[1]));
                } catch (PlayerNotFoundException e) {
                    return;
                }

                //increment stats, broadcast
                storage.set("sys.vote." + hasVoted.getUniqueId().toString(), Long.MAX_VALUE);
                storage.incrementStatsInt(hasVoted, StatsType.VOTES, 1);
                hms.getServer().broadcast(Language.getMessagePlaceholders("commandVoteVoted", true, "%PREFIX%",
                        "Vote", "%PLAYER%", hasVoted.getName()), "hms.default");

                //if the player is currently online, save his ip so that other people with same ip who cannot vote
                //can also bypass the block, also drop vote reward, or increment background reward counter
                if (hasVoted instanceof Player) {
                    Player playerHasVoted = (Player) hasVoted;
                    playerHasVoted.playSound(playerHasVoted.getLocation(), Sound.BLOCK_NOTE_PLING, 1.0f, 2.0f);
                    String address = playerHasVoted.getAddress().getAddress().toString().replace('.', 'i').substring(1);
                    storage.incrementInt("sys.vote.ip" + address, 1);
                    if (!dropCase((Player) hasVoted)) {
                        storage.incrementInt("sys.vote.reward." + hasVoted.getUniqueId(), 1);
                        playerHasVoted.sendMessage(Language.getMessagePlaceholders("commandVoteRewardInvFull", true, "%PREFIX%", "Vote"));
                    }
                } else {
                    //player not online, let him grab the reward later
                    storage.incrementInt("sys.vote.reward." + hasVoted.getUniqueId(), 1);
                }

                //check if threshold has been met to message admin
                int totalvotes = storage.incrementInt("sys.totalvotes", 1);
                if (totalvotes == hms.getConfig().getInt("command.vote.threshold", 2000)) {

                    String command = hms.getConfig().getString("command.vote.commandToExecute");
                    if (command.length() > 0) {
                        hms.getServer().dispatchCommand(hms.getServer().getConsoleSender(), command);
                    }
                }

            } else if (args[0].equalsIgnoreCase("getreward") && sender instanceof Player) {

                Player player = (Player) sender;
                int rewardAmount = storage.getInt("sys.vote.reward." + player.getUniqueId());

                if (rewardAmount == 0) {
                    player.sendMessage(Language.getMessagePlaceholders("commandVoteRewardDeny", true, "%PREFIX%", "Vote"));
                    return;
                }

                while (rewardAmount > 0 && dropCase(player)) rewardAmount--;

                //Reward could not be paid due to full inventory, send message
                if (rewardAmount > 0) {
                    player.sendMessage(Language.getMessagePlaceholders("commandVoteRewardInvFull",
                            true, "%PREFIX%", "Vote"));
                }
                storage.set("sys.vote.reward." + player.getUniqueId(), rewardAmount);

            } else showMessage(sender);

        } else showMessage(sender);
    }

    private void showMessage(CommandSender sender) {

        String playername = "";
        int rewardLeft = 0;

        if (sender instanceof Player) {
            playername = sender.getName();
            rewardLeft = storage.getInt("sys.vote.reward." + ((Player) sender).getUniqueId());
        }

        int totalVotes = storage.getInt("sys.totalvotes");
        int totalVotesThreshold = hms.getConfig().getInt("command.vote.threshold", 2000);

        String message = Language.getMessage("commandVoteTop") + "\n";

        if (playername.length() > 0) {
            message += Language.getMessagePlaceholders("commandVoteMessage", false, "%PLAYER%", playername) + "\n \n";
        } else {
            message += Language.getMessage("commandVoteMessageNotPlayer") + "\n \n";
        }

        if (totalVotes < totalVotesThreshold) {
            message += Language.getMessagePlaceholders("commandVoteUntil", false
                    , "%TOTALVOTES%", String.valueOf(totalVotes)) + "\n";
        } else {
            message += Language.getMessagePlaceholders("commandVoteReached", false,
                    "%TOTALVOTESTHRESHOLD%", String.valueOf(totalVotes)) + "\n";
        }

        if (rewardLeft > 0) {
            message += Language.getMessage("commandVoteGrabReward") + "\n";
        }

        message += Language.getMessage("lineSeparator");

        sender.sendMessage(message);
    }

    private boolean dropCase(Player player) {

        boolean hasRoom = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                hasRoom = true;
                break;
            }
        }

        if (!hasRoom) return false;

        String command = hms.getConfig().getString("command.vote.voteRewardCommand");
        command = Language.placeholderReplace(command, "%PLAYER%", player.getName());
        hms.getServer().dispatchCommand(hms.getServer().getConsoleSender(), command);
        return true;
    }
}
