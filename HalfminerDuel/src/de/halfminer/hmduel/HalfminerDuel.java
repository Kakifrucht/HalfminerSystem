package de.halfminer.hmduel;

import de.halfminer.hmduel.module.ArenaManager;
import de.halfminer.hmduel.module.ArenaQueue;
import de.halfminer.hmduel.util.Util;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HalfminerDuel extends JavaPlugin {

    private static HalfminerDuel instance;
    public static HalfminerDuel getInstance() {
        return instance;
    }

    //Modules
    private ArenaQueue arenaQueue;
    private ArenaManager arenaManager;

    public void onEnable() {

        instance = this;
        loadConfig();
        arenaManager = new ArenaManager();
        arenaQueue = new ArenaQueue();
        getServer().getPluginManager().registerEvents(new Listeners(), this);

        getLogger().info("HalfminerDuel enabled");
    }

    @Override
    public void onDisable() {

        for(Player player: getServer().getOnlinePlayers()) {
            if(arenaQueue.isInDuel(player)) arenaQueue.gameHasFinished(player, false);
        }

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("HalfminerDuel disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if(cmd.getName().equalsIgnoreCase("duel")) {
            if(sender instanceof Player) {
                Player player = (Player) sender;
                if(arenaManager.noArenaExists()) {
                    Util.sendMessage(player, "pluginDisabled");
                    return true;
                }
                if(!player.hasPermission("hmd.duel")) {
                    Util.sendMessage(player, "noPermission");
                    return true;
                }
                if(args.length != 1) Util.sendMessage(player, "help");
                else {
                    if(args[0].equalsIgnoreCase("match")) {
                        arenaQueue.matchPlayer(player);
                    } else if(args[0].equalsIgnoreCase("leave")) {
                        arenaQueue.removeFromQueue(player);
                    } else if(args[0].equalsIgnoreCase("list")) {
                        Util.sendMessage(player, "showArenaList");
                        player.sendMessage(arenaQueue.toString());
                    } else {
                        Player requested = getServer().getPlayer(args[0]);
                        if(requested != null && player.canSee(requested)) {
                            if(requested.equals(player)) Util.sendMessage(player, "duelRequestYourself");
                            else arenaQueue.requestSend(player, requested);
                        } else Util.sendMessage(player, "duelNotOnline", new String[]{"%PLAYER%",args[0]});
                    }
                    return true;
                }
            } else sender.sendMessage("This command can only be executed ingame");
            return true;
        }

        if(cmd.getName().equalsIgnoreCase("duela")) {
            if(sender instanceof Player) {
                Player player = (Player) sender;
                if(player.hasPermission("hmd.admin")) {
                    if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                        loadConfig();
                        arenaManager.loadArenasFromConfig();
                        Util.sendMessage(player,"adminSettingsReloaded");
                    }
                    else if(args.length != 2) Util.sendMessage(player, "adminHelp");
                    else {
                        switch(args[0].toLowerCase()) {
                            case "create":
                                if(arenaManager.addArena(args[1], player.getLocation())) Util.sendMessage(player,"adminCreate", new String[]{"%ARENA%",args[1]});
                                else Util.sendMessage(player, "adminCreateFailed", new String[]{"%ARENA%",args[1]}); break;
                            case "remove":
                                if(arenaManager.delArena(args[1])) Util.sendMessage(player,"adminRemove", new String[]{"%ARENA%",args[1]});
                                else Util.sendMessage(player, "adminArenaDoesntExist", new String[]{"%ARENA%",args[1]}); break;
                            case "spawna":
                                if(arenaManager.setSpawn(args[1], player.getLocation(), true)) Util.sendMessage(player,"adminSetSpawn", new String[]{"%ARENA%",args[1]});
                                else Util.sendMessage(player, "adminArenaDoesntExist", new String[]{"%ARENA%",args[1]}); break;
                            case "spawnb":
                                if(arenaManager.setSpawn(args[1], player.getLocation(), false)) Util.sendMessage(player,"adminSetSpawn", new String[]{"%ARENA%",args[1]});
                                else Util.sendMessage(player, "adminArenaDoesntExist", new String[]{"%ARENA%",args[1]}); break;
                            case "setkit":
                                if(arenaManager.setKit(args[1], player.getInventory().getArmorContents(), player.getInventory().getContents())) Util.sendMessage(player,"adminSetKit", new String[]{"%ARENA%",args[1]});
                                else Util.sendMessage(player, "adminArenaDoesntExist", new String[]{"%ARENA%",args[1]}); break;
                            default: Util.sendMessage(player, "adminHelp");
                        }
                    }
                } else Util.sendMessage(player, "noPermission");

            } else sender.sendMessage("This command can only be executed ingame");
            return true;
        }

        return false;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public ArenaQueue getArenaQueue() {
        return this.arenaQueue;
    }

    private void loadConfig() {
        saveDefaultConfig(); //Save default config.yml if not yet done
        reloadConfig(); //Make sure that if the file changed, it is reread
        getConfig().options().copyDefaults(true); //if parameters are missing, add them
        saveConfig(); //save config.yml to disk
    }

}
