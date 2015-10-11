package de.halfminer.hms.cmd;

import de.halfminer.hms.util.Language;
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
                    hms.getModSignEdit().makeCopies(player, amountToCopy);
                    player.sendMessage(Language.getMessagePlaceholderReplace("commandSigneditCopy", true, "%PREFIX%", "Hinweis", "%AMOUNT%", Byte.toString(amountToCopy)));
                    return;
                } else {
                    try {
                        byte line = Byte.parseByte(args[0]);
                        if (line > 0 && line < 5) {
                            String setTo = "";
                            if (args.length > 1) {
                                setTo = Language.arrayToString(args, 1, true);
                                if (setTo.length() > 15) setTo = setTo.substring(0, 15); //truncate if necessary
                                hms.getModSignEdit().setLine(player, line, setTo);
                            } else hms.getModSignEdit().setLine(player, line, ""); //empty line
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
        } else sender.sendMessage(Language.getMessage("notAPlayer"));

    }

    private void usageMessage(Player player) {
        player.sendMessage(Language.getMessagePlaceholderReplace("commandSigneditUsage", true, "%PREFIX%", "Hinweis"));
    }

}
