package de.halfminer.hml;

import de.halfminer.hml.cmd.*;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.land.Board;
import de.halfminer.hml.land.LandBoard;
import de.halfminer.hml.land.contract.ContractManager;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.menu.MenuCreator;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class HalfminerLand extends JavaPlugin implements MenuCreator {

    private static final String PREFIX = "Land";
    private static HalfminerLand instance;

    static HalfminerLand getInstance() {
        return instance;
    }

    private WorldGuardHelper worldGuardHelper;
    private LandStorage landStorage;

    private LandBoard board;


    @Override
    public void onEnable() {
        instance = this;

        this.worldGuardHelper = new WorldGuardHelper(getLogger(), getServer().getPluginManager());
        this.landStorage = new LandStorage(new HanStorage(this));
        reload();

        this.board = new LandBoard();
        new LandListener(board, worldGuardHelper);
        reload();

        getLogger().info("HalfminerLand enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String commandStr = command.getName();
        String[] argsTruncated = args;
        LandCommand landCommand;
        if (commandStr.equalsIgnoreCase("landtp")) {

            landCommand = new Cmdlandtp();

        } else if (commandStr.equalsIgnoreCase("hml")) {

            landCommand = new Cmdhml();

        } else {

            if (args.length == 0) {
                showUsage(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "buy":
                    landCommand = new Cmdbuy();
                    break;
                case "friend":
                    landCommand = new Cmdfriend();
                    break;
                case "fly":
                    landCommand = new Cmdfly();
                    break;
                case "info":
                    landCommand = new Cmdinfo();
                    break;
                case "sell":
                    landCommand = new Cmdsell();
                    break;
                case "teleport":
                    landCommand = new Cmdteleport();
                    break;
                case "list":
                    landCommand = new Cmdlist();
                    break;
                default:
                    showUsage(sender);
                    return true;
            }

            argsTruncated = Arrays.copyOfRange(args, 1, args.length);
        }

        if (landCommand.hasPermission(sender)) {
            landCommand.run(sender, argsTruncated);
        } else {
            MessageBuilder.create("noPermission", PREFIX).sendMessage(sender);
        }

        return true;
    }

    private void showUsage(CommandSender sender) {
        MessageBuilder.create("usage", this).togglePrefix().sendMessage(sender);
    }

    public void reload() {
        HalfminerSystem.getInstance()
                .getHalfminerManager()
                .reloadOcurred(this);
    }

    public WorldGuardHelper getWorldGuardHelper() {
        return worldGuardHelper;
    }

    public LandStorage getLandStorage() {
        return landStorage;
    }

    public void saveLandStorage() {
        landStorage.saveData();
    }

    public Board getBoard() {
        return board;
    }

    ContractManager getContractManager() {
        return board;
    }

    @Override
    public Plugin getPlugin() {
        return this;
    }
}
