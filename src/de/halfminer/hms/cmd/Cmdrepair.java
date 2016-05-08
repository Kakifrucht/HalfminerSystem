package de.halfminer.hms.cmd;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * - Repair the held item or whole inventory
 *   - Permissions for access restriction
 * - Adds configurable variable cooldown per level
 *   - Will only apply cooldown if item was actually repaired
 * - If repairing single item, checks if it is a stack (permission required)
 */
@SuppressWarnings("unused")
public class Cmdrepair extends HalfminerCommand {

    private int totalRepairs = 0;

    public Cmdrepair() {
        this.permission = "hms.repair";
    }

    @Override
    public void run(CommandSender sender, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(Language.getMessagePlaceholders("notAPlayer", true, "%PREFIX%", "Repair"));
            return;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("all") && sender.hasPermission("hms.repair.all")) {

            doRepair(player.getInventory().getItemInOffHand(), true);
            for (ItemStack item : player.getInventory().getStorageContents()) doRepair(item, true);
            for (ItemStack item : player.getInventory().getArmorContents()) doRepair(item, true);
            sender.sendMessage(Language.getMessagePlaceholders("cmdRepairDoneAll", true, "%PREFIX%", "Repair",
                    "%AMOUNT%", String.valueOf(totalRepairs)));
        } else {

            HalfminerPlayer hPlayer = storage.getPlayer(player);

            int secondsUntil = Integer.MAX_VALUE;
            if (player.hasPermission("hms.repair.nocooldown")) secondsUntil = 0;
            else {
                int lowestMultiplier = hms.getConfig().getInt("command.repair.cooldownBase", 900);
                for (int i = 6; i > 0; i--) {
                    if (player.hasPermission("hms.level." + i)) {
                        secondsUntil = lowestMultiplier;
                        break;
                    } else lowestMultiplier *= 2;
                }
                secondsUntil = Math.min(hms.getConfig().getInt("command.repair.maxCooldown", 28800), secondsUntil);
            }

            long repairTime = hPlayer.getLong(DataType.LAST_REPAIR) + secondsUntil;
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime < repairTime) {
                sender.sendMessage(Language.getMessagePlaceholders("cmdRepairCooldown", true, "%PREFIX%", "Repair",
                        "%MINUTES%", String.valueOf((repairTime - currentTime) / 60)));
                return;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (doRepair(hand, player.hasPermission("hms.repair.stacks"))) {

                hPlayer.set(DataType.LAST_REPAIR, System.currentTimeMillis() / 1000);
                sender.sendMessage(Language.getMessagePlaceholders("cmdRepairDone", true, "%PREFIX%", "Repair",
                        "%NAME%", Language.makeStringFriendly(hand.getType().toString())));
            } else sender.sendMessage(Language.getMessagePlaceholders("cmdRepairError", true, "%PREFIX%", "Repair"));
        }
    }

    private boolean doRepair(ItemStack stack, boolean repairStacks) {

        if (stack == null || stack.getType().getMaxDurability() == 0
                || stack.getDurability() == 0
                || (!repairStacks && stack.getAmount() > 1)) return false;

        stack.setDurability((short) 0);
        totalRepairs += stack.getAmount();
        return true;
    }
}
