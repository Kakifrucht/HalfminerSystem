package de.halfminer.hms.cmd;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanStorage;
import org.bukkit.command.CommandSender;

/**
 * HalfminerCommands are instantiated once a player executes the command
 */
public abstract class HalfminerCommand {

    final static HalfminerSystem hms = HalfminerSystem.getInstance();
    final HanStorage storage = (HanStorage) hms.getHandler(HandlerType.STORAGE);
    String permission = "hms.default";

    public abstract void run(CommandSender sender, String label, String[] args);

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(permission);
    }

}
