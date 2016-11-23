package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.enums.Sellable;
import de.halfminer.hms.modules.ModVerkauf;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.inventory.Inventory;

/**
 * - Sell sellable items via command
 * - Uses ModVerkauf for the actual sale
 * - Possibility to sell multiple inventories at once
 */
@SuppressWarnings("unused")
public class Cmdverkauf extends HalfminerCommand {

    private final ModVerkauf verkaufModule = (ModVerkauf) hms.getModule(ModuleType.VERKAUF);

    private Sellable toBeSold;
    private int sellCountTotal = 0;

    public Cmdverkauf() {
        this.permission = "hms.verkauf";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Verkauf");
            return;
        }

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("auto")) {

                if (!player.hasPermission("hms.verkauf.auto")) {
                    sendNoPermissionMessage("Verkauf");
                    return;
                }

                boolean toggledOn = verkaufModule.toggleAutoSell(player);
                MessageBuilder.create(hms, toggledOn ? "" +
                        "cmdVerkaufAutoOn" : "cmdVerkaufAutoOff", "Verkauf")
                        .sendMessage(player);
                return;
            }

            this.toBeSold = Sellable.getFromString(args[0]);

            if (toBeSold != null) sellLoop();
            else MessageBuilder.create(hms, "cmdVerkaufUsage", "Verkauf").sendMessage(player);

        } else MessageBuilder.create(hms, "cmdVerkaufUsage", "Verkauf").sendMessage(player);
    }

    private void sellLoop() {

        Inventory playerInv = player.getInventory();

        int sellCount = verkaufModule.sellMaterial(player, playerInv, toBeSold);
        sellCountTotal += sellCount;

        if (sellCount > 0)
            scheduler.runTaskLater(hms, this::sellLoop, 2L);
        else if (sellCountTotal > 0)
            verkaufModule.rewardPlayer(player, toBeSold, sellCountTotal);
        else MessageBuilder.create(hms, "cmdVerkaufNotInInv", "Verkauf")
                    .addPlaceholderReplace("%MATERIAL%", Language.makeStringFriendly(toBeSold.name()))
                    .sendMessage(player);
    }
}
