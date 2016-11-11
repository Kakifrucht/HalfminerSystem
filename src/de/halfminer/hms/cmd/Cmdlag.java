package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.modules.ModTps;
import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * - Information if player or server lags
 * - View other players latency/ping
 */
@SuppressWarnings("unused")
public class Cmdlag extends HalfminerCommand {

    public Cmdlag() {
        this.permission = "hms.lag";
    }

    @Override
    public void execute() {

        // determine latency
        CraftPlayer player;
        boolean showSummary = false;
        if (args.length > 0) { //get other player

            if (!sender.hasPermission("hms.lag.others")) {
                sender.sendMessage(Language.getMessagePlaceholders("noPermission", true, "%PREFIX%", "Lag"));
                return;
            }

            Player toGet = server.getPlayer(args[0]);

            if (toGet != null) {
                player = (CraftPlayer) toGet;
            } else {
                sender.sendMessage(Language.getMessagePlaceholders("playerNotOnline", true, "%PREFIX%", "Lag"));
                return;
            }

        } else if (isPlayer) {
            player = (CraftPlayer) sender;
            showSummary = true;
        } else {
            sender.sendMessage(Language.getMessage("notAPlayer"));
            return;
        }

        // get latency and tps
        int ping = player.getHandle().ping;
        double tps = ((ModTps) hms.getModule(ModuleType.TPS)).getTps();

        // values for summary, determine who is lagging
        int summaryServerLag = 0;
        boolean summaryPlayerLag = true; // set to false if player is not lagging

        String pingString;
        if (ping > 1000 || ping < 0) pingString = ChatColor.DARK_RED + "> 1000"; // ping not yet known
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

        // Send ping and tps information to player
        sender.sendMessage(Language.getMessagePlaceholders("cmdLagPlayerInfo", true, "%PREFIX%", "Lag", "%PLAYER%", player.getName(), "%LATENCY%", pingString));
        sender.sendMessage(Language.getMessagePlaceholders("cmdLagServerInfo", true, "%PREFIX%", "Lag", "%TPS%", tpsString));

        if (showSummary) { // determines the summary message, only shown when viewing own status
            if (summaryServerLag == 0 && !summaryPlayerLag)
                sender.sendMessage(Language.getMessagePlaceholders("cmdLagStable", true, "%PREFIX%", "Lag"));
            else if (summaryServerLag == 1)
                sender.sendMessage(Language.getMessagePlaceholders("cmdLagServerUnstable", true, "%PREFIX%", "Lag"));
            else if (summaryServerLag == 2)
                sender.sendMessage(Language.getMessagePlaceholders("cmdLagServerLag", true, "%PREFIX%", "Lag"));
            else
                sender.sendMessage(Language.getMessagePlaceholders("cmdLagPlayerLag", true, "%PREFIX%", "Lag"));
        }
    }
}
