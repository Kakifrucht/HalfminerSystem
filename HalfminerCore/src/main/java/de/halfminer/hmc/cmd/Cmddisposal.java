package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.entity.Player;

/**
 * - Opens portable disposal
 */
@SuppressWarnings("unused")
public class Cmddisposal extends HalfminerCommand {

    public Cmddisposal() {
        this.permission = "hmc.disposal";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Disposal");
            return;
        }

        Player p = (Player) sender;
        p.openInventory(server.createInventory(p, 36, MessageBuilder.returnMessage("cmdDisposalTitle", hmc)));
    }
}
