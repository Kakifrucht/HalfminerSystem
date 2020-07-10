package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.util.Message;
import org.bukkit.entity.Player;

/**
 * - Opens portable disposal
 */
@SuppressWarnings("unused")
public class Cmddisposal extends HalfminerCommand {

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Disposal");
            return;
        }

        Player p = (Player) sender;
        p.openInventory(server.createInventory(p, 36, Message.returnMessage("cmdDisposalTitle", hmc)));
    }
}
