package de.halfminer.hmh.cmd;

import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
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

        HalfminerPlayer player;
        try {
            player = hms.getStorageHandler().getPlayer(args[0]);
        } catch (PlayerNotFoundException e) {
            //TODO ask for confirmation with /haro add <player> -confirm
            //TODO check custom section for removal
            //TODO add check on custom section for PlayerLoginEvent
            e.sendNotFoundMessage(sender, "Land");
            return;
        }

        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(player);

        boolean success = add ? haroStorage.addPlayer(haroPlayer) : haroStorage.removePlayer(haroPlayer);
        MessageBuilder.create(success ?
                ("cmdAddRemoveSuccess" + (add ? "A" : "R"))
                : ("cmdAddRemove" + (add ? "Already" : "Not") + "Added"), hmh)
                .addPlaceholder("PLAYER", player.getName())
                .sendMessage(sender);

        if (success && !add && haroPlayer.isOnline()) {
            Player playerToRemove = player.getBase().getPlayer();
            String kickMessage = MessageBuilder.returnMessage("cmdAddRemovePlayerKick", hmh, false);
            playerToRemove.kickPlayer(kickMessage);
        }
    }
}
