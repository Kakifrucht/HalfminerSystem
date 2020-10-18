package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModTps;
import de.halfminer.hmc.module.ModuleDisabledException;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.ReflectUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * - Information if player or server lags
 * - View other players latency/ping
 */
@SuppressWarnings("unused")
public class Cmdlag extends HalfminerCommand {

    public Cmdlag() {
        this.permission = "hmc.lag";
    }

    @Override
    public void execute() {

        Player toLookup = player;
        if (args.length > 0) {

            if (!sender.hasPermission("hmc.lag.others")) {
                sendNoPermissionMessage("Lag");
                return;
            }

            //get player to lookup
            toLookup = server.getPlayer(args[0]);

            if (toLookup == null) {
                Message.create("playerNotOnline", "Lag").send(sender);
                return;
            }
        }

        // get ping/latency
        boolean playerIsLagging = true;
        if (toLookup != null) {
            int ping = ReflectUtils.getPing(toLookup);
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

            Message.create("cmdLagPlayerInfo", hmc, "Lag")
                    .addPlaceholder("%PLAYER%", toLookup.getName())
                    .addPlaceholder("%LATENCY%", pingColored)
                    .send(sender);
        }

        // get tps
        ServerStatus serverLagStatus = ServerStatus.STABLE;
        if (hmc.isModuleEnabled(ModuleType.TPS)) {

            double tps = 0;
            try {
                tps = ((ModTps) hmc.getModule(ModuleType.TPS)).getTps();
            } catch (ModuleDisabledException ignored) {}

            String tpsColored = String.valueOf(tps);
            if (tps < 12.0d) {
                tpsColored = ChatColor.RED + tpsColored;
                serverLagStatus = ServerStatus.LAGGING;
            } else if (tps < 16.0d) {
                tpsColored = ChatColor.YELLOW + tpsColored;
                serverLagStatus = ServerStatus.UNSTABLE;
            } else tpsColored = ChatColor.GREEN + tpsColored;

            Message.create("cmdLagServerInfo", hmc, "Lag")
                    .addPlaceholder("%TPS%", tpsColored)
                    .send(sender);
        }

        // determines the summary message, only shown when viewing own status
        if (isPlayer && player.equals(toLookup)) {
            String messageKey;
            if (serverLagStatus == ServerStatus.STABLE && !playerIsLagging) messageKey = "cmdLagStable";
            else if (serverLagStatus == ServerStatus.UNSTABLE) messageKey = "cmdLagServerUnstable";
            else if (serverLagStatus == ServerStatus.LAGGING) messageKey = "cmdLagServerLag";
            else messageKey = "cmdLagPlayerLag";
            Message.create(messageKey, hmc, "Lag").send(sender);
        }
    }

    private enum ServerStatus {
        STABLE,
        UNSTABLE,
        LAGGING
    }
}
