package de.halfminer.hms.cmd;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class Cmdverkauf extends BaseCommand {

    public Cmdverkauf() {
        this.permission = "hms.verkauf";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        Economy econ = HalfminerSystem.getEconomy();

        if (econ == null) {
            sender.sendMessage(Language.getMessagePlaceholders("commandVerkaufNoVault", true, "%PREFIX%", "Verkauf"));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(Language.getMessagePlaceholders("notAPlayer", true, "%PREFIX%", "Verkauf"));
            return;
        }

        Player player = (Player) sender;

        if (args.length > 0) {

            Material wanted;

            switch (args[0].toLowerCase()) {
                case "cactus":
                    wanted = Material.CACTUS;
                    break;
                case "wheat":
                    wanted = Material.WHEAT;
                    break;
                case "pumpkin":
                    wanted = Material.PUMPKIN;
                    break;
                case "brownmushroom":
                    wanted = Material.BROWN_MUSHROOM;
                    break;
                case "redmushroom":
                    wanted = Material.RED_MUSHROOM;
                    break;
                case "melon":
                    wanted = Material.MELON;
                    break;
                case "cocoa":
                    wanted = Material.COCOA;
                    break;
                case "potato":
                    wanted = Material.POTATO;
                    break;
                case "carrot":
                    wanted = Material.CARROT;
                    break;
                case "sugarcane":
                    wanted = Material.SUGAR_CANE;
                    break;
                case "netherwart":
                    wanted = Material.NETHER_WARTS;
                    break;
                default:
                    wanted = null;
                    break;
            }

            if (wanted != null) {

                double multiplier = 1.0;

                if (player.hasPermission("hms.level.5")) multiplier = 2.5d;
                else if (player.hasPermission("hms.level.4")) multiplier = 2.0d;
                else if (player.hasPermission("hms.level.3")) multiplier = 1.75d;
                else if (player.hasPermission("hms.level.2")) multiplier = 1.5d;
                else if (player.hasPermission("hms.level.1")) multiplier = 1.25d;

                int baseValue = hms.getConfig().getInt("command.verkauf." + args[0].toLowerCase(), 1);
                Inventory playerInv = player.getInventory();
                HashMap<Integer, ? extends ItemStack> items = playerInv.all(wanted);

                int count = 0;
                double paidOut;

                for (Map.Entry<Integer, ? extends ItemStack> entry : items.entrySet()) {
                    count += entry.getValue().getAmount();
                    player.getInventory().setItem(entry.getKey(), null);
                }

                player.updateInventory();
                paidOut = (count / (double) baseValue) * multiplier;

                if (paidOut > 0.0d) {

                    paidOut = Math.round(paidOut * 100) / 100.0d;
                    econ.depositPlayer(player, paidOut);
                    storage.incrementStatsDouble(player, StatsType.REVENUE, paidOut);

                    String materialFriendly = Language.makeMaterialStringFriendly(wanted);
                    player.sendMessage(Language.getMessagePlaceholders("commandVerkaufSuccess", true, "%PREFIX%", "Verkauf",
                            "%MATERIAL%", materialFriendly, "%MONEY%", String.valueOf(paidOut),
                            "%AMOUNT%", String.valueOf(count)));

                    hms.getLogger().info(Language.getMessagePlaceholders("commandVerkaufSuccessLog", false, "%PLAYER%",
                            player.getName(), "%MATERIAL%", materialFriendly, "%MONEY%", String.valueOf(paidOut),
                            "%AMOUNT%", String.valueOf(count)));
                } else {

                    player.sendMessage(Language.getMessagePlaceholders("commandVerkaufNotInInv", true, "%PREFIX%", "Verkauf",
                            "%MATERIAL%", Language.makeMaterialStringFriendly(wanted)));
                }

            } else {
                sender.sendMessage(Language.getMessagePlaceholders("commandVerkaufUsage", true, "%PREFIX%", "Verkauf"));
            }

        } else {
            sender.sendMessage(Language.getMessagePlaceholders("commandVerkaufUsage", true, "%PREFIX%", "Verkauf"));
        }
    }
}
