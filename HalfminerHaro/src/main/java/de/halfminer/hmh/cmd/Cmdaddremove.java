package de.halfminer.hmh.cmd;

import de.halfminer.hmh.HalfminerHaro;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.NameHaroPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.entity.Player;

public class Cmdaddremove extends HaroCommand {

    private final boolean add;

    public Cmdaddremove(boolean add) {
        super(add ? "add" : "remove");
        this.add = add;
    }

    @Override
    protected void execute() {

        // can only add player if game is not running
        boolean isGameRunning = haroStorage.isGameRunning();
        if (add && isGameRunning) {
            MessageBuilder.create("cmdAddRemoveGameIsRunning", hmh).sendMessage(sender);
            return;
        }

        if (args.length < 1) {
            MessageBuilder.create("cmdAddRemoveUsage", hmh)
                    .addPlaceholder("ADD", add ? "add" : "remove")
                    .sendMessage(sender);
            return;
        }

        String filteredUsername = Utils.filterNonUsernameChars(args[0]);
        if (filteredUsername.length() != args[0].length()) {
            MessageBuilder.create("playerDoesNotExist", HalfminerHaro.MESSAGE_PREFIX).sendMessage(sender);
            return;
        }

        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(args[0]);
        if (haroPlayer instanceof NameHaroPlayer) {
            // players who haven't joined before need to be confirmed with /haro add <player> -confirm
            if (add && (args.length < 2 || !args[1].equalsIgnoreCase("-confirm"))) {
                MessageBuilder.create("cmdAddRemoveConfirm", hmh)
                        .addPlaceholder("%PLAYER%", args[0])
                        .sendMessage(sender);
                return;
            }
        }

        boolean success = add ? haroStorage.addPlayer(haroPlayer) : haroStorage.removePlayer(haroPlayer);
        if (success && !add && haroPlayer.isOnline()) {
            Player playerToRemove = haroPlayer.getBase().getPlayer();
            String kickMessage = MessageBuilder.returnMessage("cmdAddRemovePlayerKick", hmh, false);
            playerToRemove.kickPlayer(kickMessage);
        }

        String messageKey = success ?
                ("cmdAddRemoveSuccess" + (add ? "A" : "R")) : ("cmdAddRemove" + (add ? "Already" : "Not") + "Added");
        MessageBuilder.create(messageKey, hmh)
                .addPlaceholder("PLAYER", haroPlayer.getName())
                .sendMessage(sender);
    }
}
