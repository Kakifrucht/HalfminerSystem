package de.halfminer.hmh.cmd;

import de.halfminer.hmh.HalfminerHaro;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.NameHaroPlayer;
import de.halfminer.hms.util.Message;
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
            Message.create("cmdAddRemoveUsage", hmh)
                    .addPlaceholder("ADD", add ? "add" : "remove")
                    .send(sender);
            return;
        }

        String filteredUsername = Utils.filterNonUsernameChars(args[0]);
        if (filteredUsername.length() != args[0].length()) {
            Message.create("playerDoesNotExist", HalfminerHaro.MESSAGE_PREFIX).send(sender);
            return;
        }

        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(args[0]);

        // players who haven't joined before need to be confirmed with /haro add <player> -confirm
        if (add && !haroPlayer.isAdded()
                && haroPlayer instanceof NameHaroPlayer
                && (args.length < 2 || !args[1].equalsIgnoreCase("-confirm"))) {

            Message.create("cmdAddRemoveConfirm", hmh)
                    .addPlaceholder("%PLAYER%", args[0])
                    .send(sender);
            return;
        }

        boolean success = false;
        if (!add && haroStorage.isGameRunning() && haroPlayer.isAdded()) {
            if (!haroPlayer.isEliminated()) {
                haroPlayer.setEliminated(true);
                success = true;
            }
        } else {
            success = add ? haroStorage.addPlayer(haroPlayer) : haroStorage.removePlayer(haroPlayer);
        }

        if (success && !add && haroPlayer.isOnline()) {
            Player playerToRemove = haroPlayer.getBase().getPlayer();
            String kickMessage = Message.returnMessage("cmdAddRemovePlayerKick", hmh, false);
            playerToRemove.kickPlayer(kickMessage);
        }

        String messageKey = success ?
                ("cmdAddRemoveSuccess" + (add ? "A" : "R")) : ("cmdAddRemove" + (add ? "Already" : "Not") + "Added");
        Message.create(messageKey, hmh)
                .addPlaceholder("PLAYER", haroPlayer.getName())
                .send(sender);
    }
}
