package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;

import java.util.logging.Level;

/**
 * - Executes Essentials /home after unblock from vote
 * - Allows usage up to 15 minutes after join
 * - Doesn't block for new users (< 300 Minutes)
 * - Doesn't block users whose ip has already voted twice
 */
@SuppressWarnings("unused")
public class Cmdhome extends HalfminerCommand {

    public Cmdhome() {
        this.permission = "hms.home";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Home");
            return;
        }

        String command = "ehome" + ' ' + Utils.arrayToString(args, 0, false);

        if (player.hasPermission("hms.moderator") || player.hasPermission("essentials.home"))
            server.dispatchCommand(player, command);
        else if (storage.getLong("vote." + player.getUniqueId().toString()) > (System.currentTimeMillis() / 1000)
                || storage.getPlayer(player).getInt(DataType.TIME_ONLINE) < 18000
                || storage.getInt("vote.ip"
                + player.getAddress().getAddress().toString().replace('.', 'i').substring(1)) > 1) {

            player.addAttachment(hms, "essentials.home", true, 0);
            server.dispatchCommand(player, command);

        } else {

            MessageBuilder.create(hms, "cmdHomeDenied", "Home")
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .sendMessage(player);

            MessageBuilder.create(hms, "cmdHomeDeniedLog")
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .logMessage(Level.INFO);
        }
    }
}
