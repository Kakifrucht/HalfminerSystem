package de.halfminer.hms.exception;

import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;

public class PlayerNotFoundException extends Exception {

    public void sendNotFoundMessage(CommandSender sendTo, String prefix) {
        sendTo.sendMessage(Language.getMessagePlaceholders("playerDoesNotExist", true, "%PREFIX%", prefix));
    }

}
