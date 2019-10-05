package de.halfminer.hmr;

import de.halfminer.hmr.http.HTTPServer;
import de.halfminer.hms.HalfminerSystem;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * HalfminerREST main class, Bukkit plugin running a lightweight REST HTTP server responding in JSON
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerREST extends JavaPlugin {

    private static HalfminerREST instance;

    public static HalfminerREST getInstance() {
        return instance;
    }

    private HTTPServer server;

    @Override
    public void onEnable() {
        instance = this;
        if (load()) {
            getLogger().info("HalfminerREST enabled");
        } else {
            getLogger().severe("HalfminerREST was not enabled properly");
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop();
            getLogger().info("HTTP server stopped");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (load()) {
                    sender.sendMessage(ChatColor.GREEN + "Reload was successful");
                } else {
                    sender.sendMessage(ChatColor.RED + "Reload unsuccessful, see console for details");
                }
                break;
            case "status":
                sender.sendMessage("HalfminerREST version " + ChatColor.GOLD + getDescription().getVersion());
                sender.sendMessage("HTTP server running? " +
                        (server != null ?
                                ChatColor.GREEN + "Yes, on port " + server.getListeningPort()
                                : ChatColor.RED + "No"));
                sender.sendMessage("Proxy mode enabled? " + (server.isProxyMode() ?
                        ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
                break;
            default:
                sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sendTo) {
        sendTo.sendMessage(ChatColor.RED + "Usage: " + ChatColor.RESET + "/hmr <reload|status>");
    }

    private boolean load() {

        HalfminerSystem.getInstance().getHalfminerManager().reload(this);

        if (server != null) {
            getLogger().info("Reloading HTTP server, stopping old instance...");
            server.stop();
            server = null;
        }

        int port = getConfig().getInt("server.port");
        if (port < 1 || port > 65535) {
            getLogger().severe("Invalid port given, HTTP server was not started");
            return false;
        }

        Set<String> whitelist = new HashSet<>(getConfig().getStringList("server.whitelistedIPs"));
        if (whitelist.size() < 1) {
            getLogger().severe("No whitelisted IP's specified, HTTP server was not started");
            return false;
        }

        boolean proxyMode = getConfig().getBoolean("server.proxyMode", false);
        try {
            server = new HTTPServer(getLogger(), port, whitelist, proxyMode);
        } catch (IOException e) {
            getLogger().severe("Couldn't bind port " + port + ", disabling");
            return false;
        }

        return true;
    }
}
