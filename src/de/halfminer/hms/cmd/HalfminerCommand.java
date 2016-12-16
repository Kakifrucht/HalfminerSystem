package de.halfminer.hms.cmd;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * HalfminerCommands are instantiated once a player executes the command.
 */
@SuppressWarnings("unused")
public abstract class HalfminerCommand extends HalfminerClass {

    protected final static HanStorage storage = (HanStorage) hms.getHandler(HandlerType.STORAGE);
    protected String permission = "hms.default";

    protected CommandSender sender;
    protected String label;
    protected String[] args;
    protected Player player;
    protected boolean isPlayer = false;

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
        MessageBuilder.create(hms, "notAPlayer", prefix).sendMessage(sender);
    }

    protected void sendNoPermissionMessage(String prefix) {
        MessageBuilder.create(hms, "noPermission", prefix).sendMessage(sender);
    }
}
