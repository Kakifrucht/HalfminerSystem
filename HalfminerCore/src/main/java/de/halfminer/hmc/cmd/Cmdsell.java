package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModSell;
import de.halfminer.hmc.module.ModuleDisabledException;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hms.util.MessageBuilder;

/**
 * - Show sell menu
 * - Allows selling item at position in menu with /sell [itemposition], first item with /sell 1
 * - Toggle automatic selling
 * - /clearcycle alias forces a new sell cycle
 */
@SuppressWarnings("unused")
public class Cmdsell extends HalfminerCommand {

    @Override
    public void execute() throws ModuleDisabledException {

        final ModSell sellModule = (ModSell) hmc.getModule(ModuleType.SELL);
        if (label.equals("clearcycle") && sender.hasPermission("hmc.sell.clearcycle")) {
            sellModule.startNewCycle();
            return;
        }

        if (!isPlayer) {
            sendNotAPlayerMessage("Sell");
            return;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("auto")) {
                if (!player.hasPermission("hmc.sell.auto")) {
                    sendNoPermissionMessage("Sell");
                    return;
                }

                boolean toggledOn = sellModule.toggleAutoSell(player);
                MessageBuilder.create(toggledOn ? "cmdSellAutoOn" : "cmdSellAutoOff", hmc, "Sell")
                        .sendMessage(player);

                return;
            } else {
                try {
                    int index = Integer.parseInt(args[0]);
                    if (sellModule.sellMaterialAndReward(index - 1, player)) {
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

        }

        sellModule.showSellMenu(player);
    }
}
