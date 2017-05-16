package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.util.MessageBuilder;

/**
 * - Shows the players gem account
 */
@SuppressWarnings("unused")
public class Cmdgems extends HalfminerCommand {


    public Cmdgems() {
        this.permission = "hmc.gems";
    }

    @Override
    protected void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Gems");
            return;
        }

        int gems = storage.getPlayer(player).getInt(DataType.GEMS);
        if (gems > 0) {
            MessageBuilder.create("cmdGemsShow", hmc, "Gems")
                    .addPlaceholderReplace("%GEMS%", String.valueOf(gems))
                    .sendMessage(player);
        } else {
            MessageBuilder.create("cmdGemsHasNone", hmc, "Gems").sendMessage(player);
        }
    }
}
