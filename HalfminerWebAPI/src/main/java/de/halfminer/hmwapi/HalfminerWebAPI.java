package de.halfminer.hmwapi;

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

    private HTTPServer server;

    @Override
    public void onEnable() {
        if (load()) getLogger().info("HalfminerWebAPI enabled successfully");
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
        //TODO status commands, config reload command
        return false;
    }

    private boolean load() {

        if (server != null) {
            getLogger().info("Reloading HTTP server, stopping old instance...");
            server.stop();
        }

        saveDefaultConfig();
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        int port = getConfig().getInt("server.port");
        Set<String> whitelist = new HashSet<>(getConfig().getStringList("server.whitelistedIPs"));
        if (whitelist.size() < 1) {
            getLogger().warning("No whitelisted IP's specified, HTTP server was not started");
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
