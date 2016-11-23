package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.modules.ModSignEdit;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.MessageBuilder;

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
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Signedit");
            return;
        }

        ModSignEdit signEdit = (ModSignEdit) hms.getModule(ModuleType.SIGN_EDIT);

        if (args.length <= 0) {
            showUsage();
            return;
        }

        if (args[0].equalsIgnoreCase("copy")) {

            byte amountToCopy = 1;

            if (args.length > 1) {
                try {
                    amountToCopy = Byte.parseByte(args[1]);
                    if (amountToCopy > 9 || amountToCopy < 1) amountToCopy = 1;
                } catch (NumberFormatException ignored) {
                }
            }

            signEdit.makeCopies(player, amountToCopy);
            MessageBuilder.create(hms, "cmdSigneditCopy", "Signedit")
                    .addPlaceholderReplace("%AMOUNT%", String.valueOf(amountToCopy))
                    .sendMessage(player);
            return;

        } else {

            try {
                byte line = Byte.parseByte(args[0]);
                if (line > 0 && line < 5) {
                    String setTo = "";
                    if (args.length > 1) {
                        setTo = Language.arrayToString(args, 1, true);
                        if (setTo.length() > 15) setTo = setTo.substring(0, 15); // truncate if necessary
                        signEdit.setLine(player, line, setTo);
                    } else signEdit.setLine(player, line, ""); // empty line

                    MessageBuilder.create(hms, "cmdSigneditSet", "Signedit")
                            .addPlaceholderReplace("%LINE%", String.valueOf(line))
                            .addPlaceholderReplace("%TEXT%", setTo)
                            .sendMessage(player);
                    return;
                } else {
                    showUsage();
                    return;
                }
            } catch (NumberFormatException ignored) {}
        }

        showUsage();
    }

    private void showUsage() {
        MessageBuilder.create(hms, "cmdSigneditUsage", "Signedit").sendMessage(player);
    }
}
