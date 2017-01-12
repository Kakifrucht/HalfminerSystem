package de.halfminer.hms.cmd;

import de.halfminer.hms.cmd.abs.HalfminerCommand;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.MessageBuilder;
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
        this.permission = "hms.repair";
    }

    @Override
    public void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage("Repair");
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("all") && sender.hasPermission("hms.repair.all")) {

            doRepair(player.getInventory().getItemInOffHand(), true);
            for (ItemStack item : player.getInventory().getStorageContents()) doRepair(item, true);
            for (ItemStack item : player.getInventory().getArmorContents()) doRepair(item, true);
            MessageBuilder.create(hms, "cmdRepairDoneAll", "Repair")
                    .addPlaceholderReplace("%AMOUNT%", String.valueOf(totalRepairs))
                    .sendMessage(sender);
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
                MessageBuilder.create(hms, "cmdRepairCooldown", "Repair")
                        .addPlaceholderReplace("%MINUTES%", String.valueOf(((repairTime - currentTime) / 60) + 1))
                        .sendMessage(sender);
                return;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (doRepair(hand, player.hasPermission("hms.repair.stacks"))) {

                hPlayer.set(DataType.LAST_REPAIR, System.currentTimeMillis() / 1000);
                MessageBuilder.create(hms, "cmdRepairDone", "Repair")
                        .addPlaceholderReplace("%NAME%", Utils.makeStringFriendly(hand.getType().toString()))
                        .sendMessage(sender);
            } else MessageBuilder.create(hms, "cmdRepairError", "Repair").sendMessage(sender);
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
