package de.halfminer.hml.cmd;

import de.halfminer.hml.LandClass;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public abstract class LandCommand extends LandClass {

    private static final String BASE_PERMISSION = "hml.cmd.";

    private final String command;

    CommandSender sender;
    String[] args;

    boolean isPlayer;
    Player player;


    LandCommand(String command) {
        super(false);
        this.command = command;
    }

    public void run(CommandSender sender, String[] args) {
        this.sender = sender;
        this.args = args;
        if (sender instanceof Player) {
            isPlayer = true;
            player = (Player) sender;
        }
        execute();
    }

    protected abstract void execute();

    public boolean hasPermission(CommandSender commandSender) {
        return commandSender.hasPermission(BASE_PERMISSION + command);
    }

    void sendNotAPlayerMessage() {
        MessageBuilder.create("notAPlayer", "Land").sendMessage(sender);
    }
}
