package de.halfminer.hms.cmd;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.modules.ModStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public abstract class BaseCommand {

    final HalfminerSystem hms = HalfminerSystem.getInstance();
    final ModStorage storage = hms.getModStorage();
    String permission = "hms.default";

    public abstract void run(CommandSender sender, Command cmd, String label, String[] args);

    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission(permission);
    }

}
