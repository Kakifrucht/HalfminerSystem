package de.halfminer.hmh.cmd;

import de.halfminer.hmh.HalfminerHaro;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.NameHaroPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.entity.Player;

/**
 * - Add or remove a player from the game.
 * - Added players can join the server before the game was started.
 * - Players can be removed during the game, which will set them eliminated and kick them if online.
 * - Players that haven't joined the server before can also be added.
 */
public class Cmdaddremove extends HaroCommand {

    private final boolean add;

    public Cmdaddremove(boolean add) {
        super(add ? "add" : "remove");
        this.add = add;
    }

    @Override
    protected void execute() {

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

        // players who haven't joined before need to be confirmed with /haro add <player> -confirm
        if (add && !haroPlayer.isAdded()
                && haroPlayer instanceof NameHaroPlayer
                && (args.length < 2 || !args[1].equalsIgnoreCase("-confirm"))) {

            MessageBuilder.create("cmdAddRemoveConfirm", hmh)
                    .addPlaceholder("%PLAYER%", args[0])
                    .sendMessage(sender);
            return;
        }

        boolean success;
        if (!add && haroStorage.isGameRunning() && haroPlayer.isAdded()) {
            haroPlayer.setEliminated(true);
            success = true;
        } else {
            success = add ? haroStorage.addPlayer(haroPlayer) : haroStorage.removePlayer(haroPlayer);
        }

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
