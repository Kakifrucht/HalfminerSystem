package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings("unused")
public class Cmdsignedit extends BaseCommand {

    public Cmdsignedit() {
        this.permission = "hms.signedit";
    }

    @Override
    @SuppressWarnings("EmptyCatchBlock")
    public void run(CommandSender sender, Command cmd, String label, String[] args) {

        if (sender instanceof Player) {

            Player player = (Player) sender;
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("copy")) {
                    byte amountToCopy = 1;
                    if (args.length > 1) {
                        try {
                            amountToCopy = Byte.parseByte(args[1]);
                            if (amountToCopy > 9 || amountToCopy < 1) amountToCopy = 1;
                        } catch (NumberFormatException e) {
                        }
                    }
                    hms.getSignEdit().makeCopies(player, amountToCopy);
                    player.sendMessage(Language.getMessagePlaceholderReplace("commandSigneditCopy", true, "%PREFIX%", "Hinweis", "%AMOUNT%", Byte.toString(amountToCopy)));
                    return;
                } else {
                    try {
                        byte line = Byte.parseByte(args[0]);
                        if (line > 0 && line < 5) {

                            String setTo = "";
                            if (args.length > 1) {
                                for (int i = 1; i < args.length; i++) setTo += args[i] + ' '; //build string
                                setTo = setTo.substring(0, setTo.length() - 1); //cut last space
                                if (setTo.length() > 15) setTo = setTo.substring(0, 15); //truncate if necessary
                                setTo = ChatColor.translateAlternateColorCodes('&', setTo); //translate colors
                                hms.getSignEdit().setLine(player, line, setTo);
                            } else hms.getSignEdit().setLine(player, line, ""); //empty line
                            player.sendMessage(Language.getMessagePlaceholderReplace("commandSigneditSet", true, "%PREFIX%", "Hinweis", "%LINE%", Byte.toString(line), "%TEXT%", setTo));
                            return;
                        } else {
                            usageMessage(player);
                            return;
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
            usageMessage(player);
        } else sender.sendMessage("This command cannot be executed from console");

    }

    private void usageMessage(Player player) {
        player.sendMessage(Language.getMessagePlaceholderReplace("commandSigneditUsage", true, "%PREFIX%", "Hinweis"));
    }

}
