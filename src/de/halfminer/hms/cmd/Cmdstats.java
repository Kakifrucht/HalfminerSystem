package de.halfminer.hms.cmd;

import de.halfminer.hms.modules.ModStorage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

@SuppressWarnings("unused")
public class Cmdstats extends BaseCommand {

    private final ModStorage storage = hms.getModStorage();

    public Cmdstats() {
        this.permission = "hms.stats";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        //TODO implement

    }
}
