package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.modules.ModSignEdit;
import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * - Copy signs, define copy amount
 * - Edit signs, define line number
 */
@SuppressWarnings("unused")
public class Cmdsignedit extends HalfminerCommand {

    public Cmdsignedit() {
        this.permission = "hms.signedit";
    }

    @Override
    @SuppressWarnings("EmptyCatchBlock")
    public void run(CommandSender sender, String label, String[] args) {

        if (sender instanceof Player) {

            Player player = (Player) sender;
            ModSignEdit signEdit = (ModSignEdit) hms.getModule(ModuleType.SIGN_EDIT);

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

                    signEdit.makeCopies(player, amountToCopy);
                    player.sendMessage(Language.getMessagePlaceholders("cmdSigneditCopy", true, "%PREFIX%", "Info", "%AMOUNT%", Byte.toString(amountToCopy)));
                    return;

                } else {

                    try {
                        byte line = Byte.parseByte(args[0]);
                        if (line > 0 && line < 5) {
                            String setTo = "";
                            if (args.length > 1) {
                                setTo = Language.arrayToString(args, 1, true);
                                if (setTo.length() > 15) setTo = setTo.substring(0, 15); //truncate if necessary
                                signEdit.setLine(player, line, setTo);
                            } else signEdit.setLine(player, line, ""); //empty line
                            player.sendMessage(Language.getMessagePlaceholders("cmdSigneditSet", true, "%PREFIX%", "Info", "%LINE%", Byte.toString(line), "%TEXT%", setTo));
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
        player.sendMessage(Language.getMessagePlaceholders("cmdSigneditUsage", true, "%PREFIX%", "Info"));
    }

}
