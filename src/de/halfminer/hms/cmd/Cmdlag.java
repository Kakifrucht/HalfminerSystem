package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;


@SuppressWarnings("unused")
public class Cmdlag extends BaseCommand {

    public Cmdlag() {
        this.permission = "hms.lag";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if(sender instanceof Player) {
            CraftPlayer player;
            if (args.length > 0) {
                Player toGet = hms.getServer().getPlayer(args[0]);
                if (toGet != null) {
                    player = (CraftPlayer) toGet;
                } else {
                    sender.sendMessage("Player not online");
                    return; //TODO messages, make it work correctly
                }
            } else {
                player = (CraftPlayer) sender;
            }
            int ping = player.getHandle().ping;
            sender.sendMessage("Ping von " + player.getName() + ": " + ping);
        } else sender.sendMessage(Language.getMessage("notAPlayer", false));

    }
}
