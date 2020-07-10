package de.halfminer.hmc.cmd;

import de.halfminer.hmc.cmd.abs.HalfminerCommand;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
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
        this.permission = "hmc.repair";
    }

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
            Message.create("cmdRepairDoneAll", hmc, "Repair")
                    .addPlaceholder("%AMOUNT%", totalRepairs)
                    .send(sender);
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
                Message.create("cmdRepairCooldown", hmc, "Repair")
                        .addPlaceholder("%MINUTES%", ((repairTime - currentTime) / 60) + 1)
                        .send(sender);
                return;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (doRepair(hand, player.hasPermission("hmc.repair.stacks"))) {

                hPlayer.set(DataType.LAST_REPAIR, System.currentTimeMillis() / 1000);
                Message.create("cmdRepairDone", hmc, "Repair")
                        .addPlaceholder("%NAME%", Utils.makeStringFriendly(hand.getType().toString()))
                        .send(sender);
            } else Message.create("cmdRepairError", hmc, "Repair").send(sender);
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
