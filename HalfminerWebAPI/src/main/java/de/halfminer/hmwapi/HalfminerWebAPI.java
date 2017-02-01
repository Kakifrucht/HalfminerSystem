package de.halfminer.hmwapi;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * HalfminerWebAPI main class
 *
 * @author Fabian Prieto Wunderlich - Kakifrucht
 */
public class HalfminerWebAPI extends JavaPlugin {

    private static HalfminerWebAPI instance;

    public static HalfminerWebAPI getInstance() {
        return instance;
    }

    private HTTPServer server;

    @Override
    public void onEnable() {
        instance = this;
        if (load()) {
            getLogger().info("HalfminerWebAPI enabled");
        } else {
            getLogger().severe("HalfminerWebAPI was not enabled properly");
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop();
            getLogger().info("Disabling HalfminerWebAPI, HTTP server stopped");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (command.getName().equalsIgnoreCase("hmwapi")
                && sender.hasPermission("hmwapi.command")) {

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
                    sender.sendMessage("HalfminerWebAPI version " + ChatColor.GOLD + getDescription().getVersion());
                    sender.sendMessage("HTTP server running? " +
                            (server != null ?
                                    ChatColor.GREEN + "Yes, on port " + server.getListeningPort()
                                    : ChatColor.RED + "No"));
                    break;
                default:
                    sendUsage(sender);
            }
            return true;
        }
        return false;
    }

    private void sendUsage(CommandSender sendTo) {
        sendTo.sendMessage(ChatColor.RED + "Usage: " + ChatColor.RESET + "/hmwapi <reload|status>");
    }

    private boolean load() {

        if (server != null) {
            getLogger().info("Reloading HTTP server, stopping old instance...");
            server.stop();
            server = null;
        }

        saveDefaultConfig();
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

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

        try {
            server = new HTTPServer(port, whitelist);
        } catch (IOException e) {
            getLogger().severe("Couldn't bind port " + port + ", disabling");
            return false;
        }
        return true;
    }
}
