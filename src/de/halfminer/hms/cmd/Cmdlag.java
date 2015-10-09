package de.halfminer.hms.cmd;

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
            CraftPlayer player = (CraftPlayer) sender;
            int ping = player.getHandle().ping;
            //TODO add
        }

    }
}
