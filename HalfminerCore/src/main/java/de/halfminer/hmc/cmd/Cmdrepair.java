package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

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


    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Repair");
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("all") && sender.hasPermission("hmc.repair.all")) {

            doRepair(player.getInventory().getItemInOffHand(), true);
            for (ItemStack item : player.getInventory().getStorageContents()) doRepair(item, true);
            for (ItemStack item : player.getInventory().getArmorContents()) doRepair(item, true);
            MessageBuilder.create("cmdRepairDoneAll", hmc, "Repair")
                    .addPlaceholder("%AMOUNT%", totalRepairs)
                    .sendMessage(sender);
        } else {

            HalfminerPlayer hPlayer = storage.getPlayer(player);

            int secondsUntil = Integer.MAX_VALUE;
            if (player.hasPermission("hmc.repair.nocooldown")) secondsUntil = 0;
            else {
                int lowestMultiplier = hmc.getConfig().getInt("command.repair.cooldownBase", 900);
                for (int i = 6; i > 0; i--) {
                    if (player.hasPermission("hms.level." + i)) {
                        secondsUntil = lowestMultiplier;
                        break;
                    } else lowestMultiplier *= 2;
                }
                secondsUntil = Math.min(hmc.getConfig().getInt("command.repair.maxCooldown", 28800), secondsUntil);
            }

            long repairTime = hPlayer.getLong(DataType.LAST_REPAIR) + secondsUntil;
            long currentTime = System.currentTimeMillis() / 1000;
            if (currentTime < repairTime) {
                MessageBuilder.create("cmdRepairCooldown", hmc, "Repair")
                        .addPlaceholder("%MINUTES%", ((repairTime - currentTime) / 60) + 1)
                        .sendMessage(sender);
                return;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (doRepair(hand, player.hasPermission("hmc.repair.stacks"))) {

                hPlayer.set(DataType.LAST_REPAIR, System.currentTimeMillis() / 1000);
                MessageBuilder.create("cmdRepairDone", hmc, "Repair")
                        .addPlaceholder("%NAME%", Utils.makeStringFriendly(hand.getType().toString()))
                        .sendMessage(sender);
            } else MessageBuilder.create("cmdRepairError", hmc, "Repair").sendMessage(sender);
        }
    }

    private boolean doRepair(ItemStack stack, boolean repairStacks) {

        if (stack == null || (!repairStacks && stack.getAmount() > 1)) {
            return false;
        }

        ItemMeta itemMeta = stack.getItemMeta();
        if (itemMeta instanceof Damageable) {

            Damageable damageable = (Damageable) itemMeta;

            if (damageable.getDamage() > 0) {
                damageable.setDamage(0);
                stack.setItemMeta(itemMeta);
                totalRepairs += stack.getAmount();
                return true;
            }
        }

        return false;
    }
}
