package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.module.ModuleType;
import de.halfminer.hmc.module.ModSell;
import de.halfminer.hms.util.MessageBuilder;

/**
 * - Show sell menu
 * - Toggle automatic selling
 * - /clearcycle alias forces a new sell cycle
 */
@SuppressWarnings("unused")
public class Cmdsell extends HalfminerCommand {

    private final ModSell sellModule = (ModSell) hmc.getModule(ModuleType.SELL);

    public Cmdsell() {
        this.permission = "hmc.sell";
    }

    @Override
    public void execute() {

        if (label.equals("clearcycle") && sender.hasPermission("hmc.sell.clearcycle")) {
            sellModule.startNewCycle();
            return;
        }

        if (!isPlayer) {
            sendNotAPlayerMessage("Sell");
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("auto")) {

            if (!player.hasPermission("hmc.sell.auto")) {
                sendNoPermissionMessage("Sell");
                return;
            }

            boolean toggledOn = sellModule.toggleAutoSell(player);
            MessageBuilder.create(toggledOn ? "cmdSellAutoOn" : "cmdSellAutoOff", hmc, "Sell")
                    .sendMessage(player);

        } else {
            sellModule.showSellMenu(player);
        }
    }
}
