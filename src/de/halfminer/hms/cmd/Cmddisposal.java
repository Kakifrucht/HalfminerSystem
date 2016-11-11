package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.entity.Player;

/**
 * - Opens portable disposal
 */
@SuppressWarnings("unused")
public class Cmddisposal extends HalfminerCommand {

    public Cmddisposal() {
        this.permission = "hms.disposal";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sender.sendMessage(Language.getMessage("notAPlayer"));
            return;
        }

        Player p = (Player) sender;
        p.openInventory(server.createInventory(p, 36, Language.getMessage("cmdDisposalTitle")));
    }
}
