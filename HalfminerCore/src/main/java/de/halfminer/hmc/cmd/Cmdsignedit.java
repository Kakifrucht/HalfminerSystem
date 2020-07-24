package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

/**
 * - Edit specific lines of signs by looking at them while executing the command
 */
@SuppressWarnings("unused")
public class Cmdsignedit extends HalfminerCommand {

    private final static String PREFIX = "Signedit";


    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage(PREFIX);
            return;
        }

        if (args.length <= 0) {
            showUsage();
            return;
        }

        int lineNumber = -1;
        String lineText = "";
        try {
            lineNumber = Integer.parseInt(args[0]) - 1;
        } catch (NumberFormatException ignored) {}

        if (lineNumber < 0 || lineNumber > 3) {
            showUsage();
            return;
        }

        if (args.length > 1) {
            lineText = Utils.arrayToString(args, 1, true);
        }

        Block block = player.getTargetBlock(null, 5);
        if (!(block.getState() instanceof Sign)) {
            Message.create("cmdSigneditLookAtSign", hmc, PREFIX).send(player);
            return;
        }

        Sign sign = (Sign) block.getState();
        sign.setLine(lineNumber, lineText);
        sign.update();

        String messageKey = lineText.length() > 15 ? "cmdSigneditLinePastedWarn" : "cmdSigneditLinePasted";
        Message.create(messageKey, hmc, PREFIX).send(player);
    }

    private void showUsage() {
        Message.create("cmdSigneditUsage", hmc, PREFIX).send(player);
    }
}
