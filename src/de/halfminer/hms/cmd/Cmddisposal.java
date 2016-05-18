package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
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
    public void run(CommandSender sender, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Language.getMessage("notAPlayer"));
            return;
        }

        Player p = (Player) sender;

        p.openInventory(server.createInventory(p, 36, Language.getMessage("cmdDisposalTitle")));
    }
}
