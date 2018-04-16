package de.halfminer.hml;

import de.halfminer.hml.cmd.*;
import de.halfminer.hml.land.Board;
import de.halfminer.hml.land.LandBoard;
import de.halfminer.hml.land.contract.ContractManager;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class HalfminerLand extends JavaPlugin {

    private static final String PREFIX = "Land";
    private static HalfminerLand instance;

    static HalfminerLand getInstance() {
        return instance;
    }

    private WorldGuardHelper worldGuardHelper;
    private HanStorage landStorage;

    private LandBoard board;


    @Override
    public void onEnable() {
        instance = this;

        this.worldGuardHelper = new WorldGuardHelper(getLogger(), getServer().getPluginManager());
        this.landStorage = new HanStorage(this);
        reload();

        this.board = new LandBoard();
        new LandListener(board, worldGuardHelper);
        reload();

        getLogger().info("HalfminerLand enabled");
    }

    @Override
    public void onDisable() {}

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
                case "free":
                    landCommand = new Cmdfree();
                    break;
                case "friend":
                    landCommand = new Cmdfriend();
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

    public HanStorage getLandStorage() {
        return landStorage;
    }

    public void saveLandStorage() {
        landStorage.saveConfig();
    }

    Board getBoard() {
        return board;
    }

    ContractManager getContractManager() {
        return board;
    }
}
