package de.halfminer.hms.cmd;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanStorage;
import org.bukkit.command.CommandSender;

/**
 * HalfminerCommands are instantiated once a player executes the command.
 */
@SuppressWarnings("unused")
public abstract class HalfminerCommand extends HalfminerClass {

    final static HanStorage storage = (HanStorage) hms.getHandler(HandlerType.STORAGE);
    String permission = "hms.default";

    @SuppressWarnings("UnusedParameters")
    public abstract void run(CommandSender sender, String label, String[] args);

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(permission);
    }
}
