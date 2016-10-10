package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.enums.Sellable;
import de.halfminer.hms.modules.ModVerkauf;
import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * - Sell sellable items via command
 * - Uses ModVerkauf for the actual sale
 * - Possibility to sell multiple inventories at once
 */
@SuppressWarnings("unused")
public class Cmdverkauf extends HalfminerCommand {

    private final ModVerkauf verkaufModule = (ModVerkauf) hms.getModule(ModuleType.VERKAUF);

    private Player player;
    private Sellable toBeSold;
    private int sellCountTotal = 0;

    public Cmdverkauf() {
        this.permission = "hms.verkauf";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Language.getMessagePlaceholders("notAPlayer", true, "%PREFIX%", "Verkauf"));
            return;
        }

        player = (Player) sender;

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("auto")) {

                if (!player.hasPermission("hms.verkauf.auto")) {
                    player.sendMessage(Language.getMessagePlaceholders("noPermission", true, "%PREFIX%", "Verkauf"));
                    return;
                }

                boolean toggledOn = verkaufModule.toggleAutoSell(player);

                if (toggledOn)
                    player.sendMessage(Language.getMessagePlaceholders("cmdVerkaufAutoOn", true, "%PREFIX%", "Verkauf"));
                else
                    player.sendMessage(Language.getMessagePlaceholders("cmdVerkaufAutoOff", true, "%PREFIX%", "Verkauf"));
                return;
            }

            this.toBeSold = Sellable.getFromString(args[0]);

            if (toBeSold != null) sellLoop();
            else player.sendMessage(Language.getMessagePlaceholders("cmdVerkaufUsage", true, "%PREFIX%", "Verkauf"));

        } else {
            player.sendMessage(Language.getMessagePlaceholders("cmdVerkaufUsage", true, "%PREFIX%", "Verkauf"));
        }
    }

    private void sellLoop() {

        Inventory playerInv = player.getInventory();

        int sellCount = verkaufModule.sellMaterial(player, playerInv, toBeSold);
        sellCountTotal += sellCount;

        if (sellCount > 0)
            scheduler.runTaskLater(hms, this::sellLoop, 2L);
        else if (sellCountTotal > 0)
            verkaufModule.rewardPlayer(player, toBeSold, sellCountTotal);
        else
            player.sendMessage(Language.getMessagePlaceholders("cmdVerkaufNotInInv", true, "%PREFIX%", "Verkauf",
                    "%MATERIAL%", Language.makeStringFriendly(toBeSold.name())));
    }
}
