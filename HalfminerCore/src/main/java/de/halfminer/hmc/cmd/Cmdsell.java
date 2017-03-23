package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hmc.enumerator.ModuleType;
import de.halfminer.hmc.enumerator.Sellable;
import de.halfminer.hmc.modules.ModSell;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.inventory.Inventory;

/**
 * - Sell sellable items via command
 * - Uses ModSell for the actual sale
 * - Possibility to sell multiple inventories at once
 */
@SuppressWarnings("unused")
public class Cmdsell extends HalfminerCommand {

    private final ModSell sellModule = (ModSell) hmc.getModule(ModuleType.SELL);

    private Sellable toBeSold;
    private int sellCountTotal = 0;

    public Cmdsell() {
        this.permission = "hmc.sell";
    }

    @Override
    public void execute() {

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
            }

            this.toBeSold = Sellable.getFromString(args[0]);

            if (toBeSold != null) sellLoop();
            else MessageBuilder.create("cmdSellUsage", hmc, "Sell").sendMessage(player);

        } else MessageBuilder.create("cmdSellUsage", hmc, "Sell").sendMessage(player);
    }

    private void sellLoop() {

        Inventory playerInv = player.getInventory();

        int sellCount = sellModule.sellMaterial(player, playerInv, toBeSold);
        sellCountTotal += sellCount;

        if (sellCount > 0)
            scheduler.runTaskLater(hmc, this::sellLoop, 2L);
        else if (sellCountTotal > 0)
            sellModule.rewardPlayer(player, toBeSold, sellCountTotal);
        else MessageBuilder.create("cmdSellNotInInv", hmc, "Sell")
                    .addPlaceholderReplace("%MATERIAL%", Utils.makeStringFriendly(toBeSold.name()))
                    .sendMessage(player);
    }
}
