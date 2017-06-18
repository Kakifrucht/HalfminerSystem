package de.halfminer.hms.handler.storage;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;

/**
 * Thrown if a Player could not be found via {@link de.halfminer.hms.handler.HanStorage}.
 */
public class PlayerNotFoundException extends Exception {

    public void sendNotFoundMessage(CommandSender sendTo, String prefix) {
        MessageBuilder.create("playerDoesNotExist", prefix).sendMessage(sendTo);
    }
}
