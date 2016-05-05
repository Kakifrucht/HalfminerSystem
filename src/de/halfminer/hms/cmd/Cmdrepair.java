package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by fabpw on 01.05.2016.
 */
public class Cmdrepair extends HalfminerCommand {

    public Cmdrepair() {
        this.permission = "hms.repair";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (sender instanceof Player) {
            sender.sendMessage(Language.getMessagePlaceholders("", true, "Repair"));
            return;
        }

        Player player = (Player) sender;

        //player.getInventory().getItemInMainHand().

        //TODO new DataType, use hms.level for adding time

    }
}
