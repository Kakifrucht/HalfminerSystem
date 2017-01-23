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
        if (load()) {
            getLogger().info("HalfminerWebAPI enabled successfully");
        } else {
            getLogger().warning("Couldn't bind port, plugin disabled");
            this.setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        server.stop();
        getLogger().info("Disabling HalfminerWebAPI, HTTP server stopped");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        //TODO status commands
        return false;
    }

    private boolean load() {

        if (server != null) {
            server.stop();
        }

        saveDefaultConfig();
        reloadConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();

        int port = getConfig().getInt("server.port");
        Set<String> whitelist = new HashSet<>(getConfig().getStringList("server.whitelist"));
        if (whitelist.size() < 1) {
            //TODO
            return false;
        }

        try {
            server = new HTTPServer(port, whitelist);
        } catch (IOException e) {
            e.printStackTrace();
            //TODO
            return false;
        }
        return true;
    }
}
