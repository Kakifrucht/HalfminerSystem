package de.halfminer.hms.cmd;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * HalfminerCommands are instantiated once a player executes the command.
 */
@SuppressWarnings("unused")
public abstract class HalfminerCommand extends HalfminerClass {

    final static HanStorage storage = (HanStorage) hms.getHandler(HandlerType.STORAGE);
    String permission = "hms.default";

    CommandSender sender;
    String label;
    String[] args;
    Player player;
    boolean isPlayer = false;

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

    public abstract void execute();

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(permission);
    }
}
