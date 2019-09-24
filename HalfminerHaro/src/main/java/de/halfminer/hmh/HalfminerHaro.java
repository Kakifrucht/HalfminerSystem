package de.halfminer.hmh;

import de.halfminer.hmh.cmd.*;
import de.halfminer.hmh.data.HaroStorage;
import de.halfminer.hmh.tasks.TimeCheckTask;
import de.halfminer.hmh.tasks.TitleUpdateTask;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

/**
 * HalfminerHaro main class, <i>Ha</i>lfminer <i>Ro</i>leplay (<i>Haro</i>) plugin, that converts a
 * given server into a Haro gamemode server.
 *
 * @author Fabian Prieto Wunderlich / Kakifrucht
 */
public class HalfminerHaro extends JavaPlugin {

    private static HalfminerHaro instance;

    static HalfminerHaro getInstance() {
        return instance;
    }

    private HaroStorage haroStorage;
    private TitleUpdateTask titleUpdateTask;


    @Override
    public void onEnable() {
        instance = this;

        reload();

        this.haroStorage = new HaroStorage();
        this.titleUpdateTask = new TitleUpdateTask();

        new HaroListeners();
        new TimeCheckTask();

        getLogger().info("HalfminerHaro enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        final String MESSAGE_PREFIX = "Haro";

        if (!sender.hasPermission("hmh.admin")) {
            MessageBuilder.create("noPermission", MESSAGE_PREFIX).sendMessage(sender);
            return true;
        }

        String[] argsTruncated = args;
        HaroCommand haroCommand;

        if (label.equalsIgnoreCase("harospawn")) {
            haroCommand = new Cmdsetspawn(true);
        } else {

            if (args.length == 0) {
                sendUsage(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "add":
                    haroCommand = new Cmdaddremove(true);
                    break;
                case "end":
                    haroCommand = new Cmdend();
                    break;
                case "remove":
                    haroCommand = new Cmdaddremove(false);
                    break;
                case "addtime":
                    haroCommand = new Cmdaddtime();
                    break;
                case "setspawn":
                    haroCommand = new Cmdsetspawn(false);
                    break;
                case "start":
                    haroCommand = new Cmdstart();
                    break;
                case "status":
                    haroCommand = new Cmdstatus();
                    break;
                case "save":
                    haroStorage.storeDataOnDisk();
                    MessageBuilder.create("dataSaved", this, MESSAGE_PREFIX).sendMessage(sender);
                    return true;
                case "reload":
                    reload();
                    MessageBuilder.create("pluginReloaded", MESSAGE_PREFIX)
                            .addPlaceholder("%PLUGINNAME%", getName())
                            .sendMessage(sender);
                    return true;
                default:
                    sendUsage(sender);
                    return true;
            }

            argsTruncated = Arrays.copyOfRange(args, 1, args.length);
        }

        if (haroCommand.hasPermission(sender)) {
            haroCommand.run(sender, argsTruncated);
        } else {
            MessageBuilder.create("noPermission", MESSAGE_PREFIX).sendMessage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        MessageBuilder.create("cmdUsage", this)
                .addPlaceholder("VERSION", getDescription().getVersion())
                .addPlaceholder("SYSTEMVERSION", HalfminerSystem.getInstance().getDescription().getVersion())
                .sendMessage(sender);
    }

    public HaroStorage getHaroStorage() {
        return haroStorage;
    }

    public TitleUpdateTask getTitleUpdateTask() {
        return titleUpdateTask;
    }

    private void reload() {
        HalfminerSystem.getInstance()
                .getHalfminerManager()
                .reload(this);
    }
}
