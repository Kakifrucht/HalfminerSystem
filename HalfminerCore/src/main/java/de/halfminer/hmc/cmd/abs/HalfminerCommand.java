package de.halfminer.hmc.cmd.abs;

import de.halfminer.hmc.CoreClass;
import de.halfminer.hms.handlers.HanStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * HalfminerCommands are instantiated once a player executes the command.
 */
@SuppressWarnings("unused")
public abstract class HalfminerCommand extends CoreClass {

    protected final static HanStorage storage = hms.getStorageHandler();
    protected String permission = "hmc.default";

    protected CommandSender sender;
    protected String label;
    protected String[] args;
    protected Player player;
    protected boolean isPlayer = false;

    public HalfminerCommand() {
        super(false);
    }

    public void run(CommandSender sender, String label, String[] args) {
        this.sender = sender;
        this.label = label;
        this.args = args;
        if (sender instanceof Player) {
            isPlayer = true;
            player = (Player) sender;
        }
        execute();
    }

    protected abstract void execute();

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(permission);
    }

    protected void sendNotAPlayerMessage(String prefix) {
        MessageBuilder.create("notAPlayer", prefix).sendMessage(sender);
    }

    protected void sendNoPermissionMessage(String prefix) {
        MessageBuilder.create("noPermission", prefix).sendMessage(sender);
    }
}
