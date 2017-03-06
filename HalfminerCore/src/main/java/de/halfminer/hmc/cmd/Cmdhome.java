package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
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
        this.permission = "hmc.home";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Home");
            return;
        }

        String command = "ehome" + ' ' + Utils.arrayToString(args, 0, false);

        if (player.hasPermission("hmc.moderator") || player.hasPermission("essentials.home"))
            server.dispatchCommand(player, command);
        else if (coreStorage.getLong("vote." + player.getUniqueId().toString()) > (System.currentTimeMillis() / 1000)
                || storage.getPlayer(player).getInt(DataType.TIME_ONLINE) < 18000
                || coreStorage.getInt("vote.ip"
                + player.getAddress().getAddress().toString().replace('.', 'i').substring(1)) > 1) {

            player.addAttachment(hmc, "essentials.home", true, 0);
            server.dispatchCommand(player, command);

        } else {

            MessageBuilder.create("cmdHomeDenied", hmc, "Home")
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .sendMessage(player);

            MessageBuilder.create("cmdHomeDeniedLog", hmc)
                    .addPlaceholderReplace("%PLAYER%", player.getName())
                    .logMessage(Level.INFO);
        }
    }
}
