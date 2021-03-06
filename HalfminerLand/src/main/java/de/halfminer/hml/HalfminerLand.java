package de.halfminer.hml;

import de.halfminer.hml.cmd.*;
import de.halfminer.hml.data.LandStorage;
import de.halfminer.hml.land.Board;
import de.halfminer.hml.land.LandBoard;
import de.halfminer.hml.land.contract.ContractManager;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.menu.MenuCreator;
import de.halfminer.hms.util.Message;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * HalfminerLand main class, land management plugin for Bukkit/Spigot plugin, using WorldGuard as protection backend.
 *
 * @author Fabian Prieto Wunderlich / Kakifrucht
 */
public class HalfminerLand extends JavaPlugin implements MenuCreator {

    private static final String PREFIX = "Land";
    private static HalfminerLand instance;

    public static HalfminerLand getInstance() {
        return instance;
    }

    private WorldGuardHelper worldGuardHelper;
    private LandStorage landStorage;

    private LandBoard board;


    @Override
    public void onEnable() {
        instance = this;

        this.worldGuardHelper = new WorldGuardHelper(getLogger());
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
                case "b":
                    landCommand = new Cmdbuy();
                    break;
                case "friend":
                case "fr":
                    landCommand = new Cmdfriend();
                    break;
                case "fly":
                case "f":
                    landCommand = new Cmdfly();
                    break;
                case "info":
                case "i":
                    landCommand = new Cmdinfo();
                    break;
                case "sell":
                case "s":
                    landCommand = new Cmdsell();
                    break;
                case "teleport":
                case "t":
                    landCommand = new Cmdteleport();
                    break;
                case "list":
                case "l":
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
            Message.create("noPermission", PREFIX).send(sender);
        }

        return true;
    }

    private void showUsage(CommandSender sender) {
        Message.create("usage", this).togglePrefix().send(sender);
    }

    public void reload() {
        HalfminerSystem.getInstance()
                .getHalfminerManager()
                .reload(this);
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
