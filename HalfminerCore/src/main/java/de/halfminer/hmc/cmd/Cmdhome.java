package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
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

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Home");
            return;
        }

        String command = "ehome " + Utils.arrayToString(args, 0, false);

        if (player.hasPermission("hmc.moderator") || player.hasPermission("essentials.home")) {
            server.dispatchCommand(player, command);
        } else {
            HalfminerPlayer hPlayer = storage.getPlayer(player);
            // add temporary permission if player has voted, is new or the IP has already voted twice
            if (coreStorage.getLong("vote." + player.getUniqueId().toString()) > (System.currentTimeMillis() / 1000)
                    || hPlayer.getInt(DataType.TIME_ONLINE) < 18000
                    || coreStorage.getInt("vote.ip" + hPlayer.getIPAddress().replace('.', 'i')) > 1) {

                player.addAttachment(hmc, "essentials.home", true, 0);
                server.dispatchCommand(player, command);
            } else {

                MessageBuilder.create("cmdHomeDenied", hmc, "Home")
                        .addPlaceholder("%PLAYER%", player.getName())
                        .sendMessage(player);

                MessageBuilder.create("cmdHomeDeniedLog", hmc)
                        .addPlaceholder("%PLAYER%", player.getName())
                        .logMessage(Level.INFO);
            }
        }
    }
}
