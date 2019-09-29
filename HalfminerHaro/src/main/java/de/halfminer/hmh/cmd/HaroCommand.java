package de.halfminer.hmh.cmd;

import de.halfminer.hmh.HalfminerHaro;
import de.halfminer.hmh.HaroClass;
import de.halfminer.hmh.data.HaroStorage;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Abstract base class for every Haro command.
 */
public abstract class HaroCommand extends HaroClass {

    private static final String BASE_PERMISSION = "hmh.cmd.";

    final HaroStorage haroStorage = hmh.getHaroStorage();

    private final String command;

    CommandSender sender;
    String[] args;

    boolean isPlayer;
    Player player;


    HaroCommand(String command) {
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
        MessageBuilder.create("notAPlayer", HalfminerHaro.MESSAGE_PREFIX).sendMessage(sender);
    }
}
