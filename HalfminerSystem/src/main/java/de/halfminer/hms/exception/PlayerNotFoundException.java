package de.halfminer.hms.exception;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;

/**
 * Thrown if a Player could not be found
 */
public class PlayerNotFoundException extends Exception {

    public void sendNotFoundMessage(CommandSender sendTo, String prefix) {
        MessageBuilder.create("playerDoesNotExist", prefix).sendMessage(sendTo);
    }
}
