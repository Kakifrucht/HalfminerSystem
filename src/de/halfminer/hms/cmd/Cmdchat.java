package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

@SuppressWarnings("unused")
public class Cmdchat extends BaseCommand {

    public Cmdchat() {
        this.permission = "hms.chat";
    }

    @Override
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (args.length > 0) {

            if (args[1].equalsIgnoreCase("clear")) {
                String message = ChatColor.RESET.toString();
                for (int i = 0; i < 100; i++) {
                    hms.getServer().broadcast(message, "hms.default");
                }
                sender.sendMessage(Language.getMessagePlaceholderReplace("commandChatCleared", true, "%PREFIX%", "Chat"));
            }
        }
    }
}
