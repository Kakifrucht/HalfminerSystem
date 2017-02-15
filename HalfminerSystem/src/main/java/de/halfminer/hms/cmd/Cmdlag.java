package de.halfminer.hms.cmd;

import de.halfminer.hms.cmd.abs.HalfminerCommand;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.modules.ModTps;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.NMSUtils;
import org.bukkit.ChatColor;
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

        Player toLookup = player;
        if (args.length > 0) {

            if (!sender.hasPermission("hms.lag.others")) {
                sendNoPermissionMessage("Lag");
                return;
            }

            //get player to lookup
            toLookup = server.getPlayer(args[0]);

            if (toLookup == null) {
                MessageBuilder.create("playerNotOnline", "Lag").sendMessage(sender);
                return;
            }
        }

        // get ping/latency
        boolean playerIsLagging = true;
        if (toLookup != null) {
            int ping = NMSUtils.getPing(toLookup);
            String pingColored;
            if (ping > 1000 || ping < 0) {
                // ping not known (e.g. just logged in)
                pingColored = ChatColor.DARK_RED + "> 1000";
            } else {
                pingColored = String.valueOf(ping);
                if (ping > 200) pingColored = ChatColor.RED + pingColored;
                else if (ping > 100) pingColored = ChatColor.YELLOW + pingColored;
                else {
                    pingColored = ChatColor.GREEN + pingColored;
                    playerIsLagging = false;
                }
            }

            MessageBuilder.create("cmdLagPlayerInfo", hms, "Lag")
                    .addPlaceholderReplace("%PLAYER%", toLookup.getName())
                    .addPlaceholderReplace("%LATENCY%", pingColored)
                    .sendMessage(sender);
        }

        // get tps
        double tps = ((ModTps) hms.getModule(ModuleType.TPS)).getTps();
        ServerStatus serverLagStatus = ServerStatus.STABLE;
        String tpsColored = String.valueOf(tps);
        if (tps < 12.0d) {
            tpsColored = ChatColor.RED + tpsColored;
            serverLagStatus = ServerStatus.LAGGING;
        } else if (tps < 16.0d) {
            tpsColored = ChatColor.YELLOW + tpsColored;
            serverLagStatus = ServerStatus.UNSTABLE;
        } else tpsColored = ChatColor.GREEN + tpsColored;

        MessageBuilder.create("cmdLagServerInfo", hms, "Lag")
                .addPlaceholderReplace("%TPS%", tpsColored)
                .sendMessage(sender);

        // determines the summary message, only shown when viewing own status
        if (isPlayer && player.equals(toLookup)) {
            String messageKey;
            if (serverLagStatus == ServerStatus.STABLE && !playerIsLagging) messageKey = "cmdLagStable";
            else if (serverLagStatus == ServerStatus.UNSTABLE) messageKey = "cmdLagServerUnstable";
            else if (serverLagStatus == ServerStatus.LAGGING) messageKey = "cmdLagServerLag";
            else messageKey = "cmdLagPlayerLag";
            MessageBuilder.create(messageKey, hms, "Lag").sendMessage(sender);
        }
    }

    private enum ServerStatus {
        STABLE,
        UNSTABLE,
        LAGGING
    }
}
