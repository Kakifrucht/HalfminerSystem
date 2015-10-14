package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
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

        //determine latency
        CraftPlayer player;
        boolean showSummary = false;
        if (args.length > 0) { //get other player

            if (!sender.hasPermission("hms.lag.others")) {
                sender.sendMessage(Language.getMessagePlaceholderReplace("noPermission", true, "%PREFIX%", "Lag"));
                return;
            }

            Player toGet = hms.getServer().getPlayer(args[0]);

            if (toGet != null) {
                player = (CraftPlayer) toGet;
            } else {
                sender.sendMessage(Language.getMessagePlaceholderReplace("playerNotOnline", true, "%PREFIX%", "Lag"));
                return;
            }

        } else if (sender instanceof Player) {
            player = (CraftPlayer) sender;
            showSummary = true;
        } else {
            sender.sendMessage(Language.getMessage("notAPlayer"));
            return;
        }

        //get latency and tps
        int ping = player.getHandle().ping;
        double tps = hms.getModTps().getTps();

        //values for summary, determine who is lagging
        int summaryServerLag = 0;
        boolean summaryPlayerLag = true; //set to false if player is not lagging

        String pingString;
        if (ping > 1000 || ping < 0) pingString = ChatColor.DARK_RED + "> 1000"; //ping not yet known
        else {
            pingString = String.valueOf(ping);
            if (ping > 200) pingString = ChatColor.RED + pingString;
            else if (ping > 100) pingString = ChatColor.YELLOW + pingString;
            else {
                pingString = ChatColor.GREEN + pingString;
                summaryPlayerLag = false;
            }
        }

        String tpsString = String.valueOf(tps);
        if (tps < 12.0d) {
            tpsString = ChatColor.RED + tpsString;
            summaryServerLag = 2;
        } else if (tps < 16.0d) {
            tpsString = ChatColor.YELLOW + tpsString;
            summaryServerLag = 1;
        } else tpsString = ChatColor.GREEN + tpsString;

        //Send ping and tps information to player
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandLagPlayerInfo", true, "%PREFIX%", "Lag", "%PLAYER%", player.getName(), "%LATENCY%", pingString));
        sender.sendMessage(Language.getMessagePlaceholderReplace("commandLagServerInfo", true, "%PREFIX%", "Lag", "%TPS%", tpsString));

        if (showSummary) { //determines the summary message, only shown when viewing own status
            if (summaryServerLag == 0 && !summaryPlayerLag)
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandLagStable", true, "%PREFIX%", "Lag"));
            else if (summaryServerLag == 1)
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandLagServerUnstable", true, "%PREFIX%", "Lag"));
            else if (summaryServerLag == 2)
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandLagServerLag", true, "%PREFIX%", "Lag"));
            else
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandLagPlayerLag", true, "%PREFIX%", "Lag"));
        }
    }
}
