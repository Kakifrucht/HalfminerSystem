package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.enums.Sellable;
import de.halfminer.hms.modules.ModSell;
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

    private final ModSell sellModule = (ModSell) hms.getModule(ModuleType.SELL);

    private Sellable toBeSold;
    private int sellCountTotal = 0;

    public Cmdsell() {
        this.permission = "hms.sell";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Sell");
            return;
        }

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("auto")) {

                if (!player.hasPermission("hms.sell.auto")) {
                    sendNoPermissionMessage("Sell");
                    return;
                }

                boolean toggledOn = sellModule.toggleAutoSell(player);
                MessageBuilder.create(hms, toggledOn ? "cmdSellAutoOn" : "cmdSellAutoOff", "Sell")
                        .sendMessage(player);
                return;
            }

            this.toBeSold = Sellable.getFromString(args[0]);

            if (toBeSold != null) sellLoop();
            else MessageBuilder.create(hms, "cmdSellUsage", "Sell").sendMessage(player);

        } else MessageBuilder.create(hms, "cmdSellUsage", "Sell").sendMessage(player);
    }

    private void sellLoop() {

        Inventory playerInv = player.getInventory();

        int sellCount = sellModule.sellMaterial(player, playerInv, toBeSold);
        sellCountTotal += sellCount;

        if (sellCount > 0)
            scheduler.runTaskLater(hms, this::sellLoop, 2L);
        else if (sellCountTotal > 0)
            sellModule.rewardPlayer(player, toBeSold, sellCountTotal);
        else MessageBuilder.create(hms, "cmdSellNotInInv", "Sell")
                    .addPlaceholderReplace("%MATERIAL%", Utils.makeStringFriendly(toBeSold.name()))
                    .sendMessage(player);
    }
}
