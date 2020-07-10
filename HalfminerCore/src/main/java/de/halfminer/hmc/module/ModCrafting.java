package de.halfminer.hmc.module;

import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * - Add minimum required amounts per item on a crafting field for vanilla recipes
 *   - For example, amount set for tnt at 8 requires at least 8 sand/gunpowder on every field in recipe
 */
@SuppressWarnings("unused")
public class ModCrafting extends HalfminerModule implements Listener {

    private Map<Material, Integer> craftAmountMap;


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent e) {

        CraftingInventory inventory = e.getInventory();
        ItemStack result = inventory.getResult();
        if (result != null && craftAmountMap.containsKey(result.getType())) {

            ItemStack[] craftMatrix = inventory.getMatrix();
            int amount = craftAmountMap.get(result.getType());
            int canCraftAmount = canBeCraftedTimes(inventory.getMatrix(), amount);

            if (canCraftAmount > 0) {

                if (e.isShiftClick()) {
                    if (!e.getClick().isKeyboardClick()) {
                        ItemStack resultCloned = result.clone();
                        resultCloned.setAmount(canCraftAmount);

                        if (e.getWhoClicked().getInventory().addItem(resultCloned).isEmpty()) {
                            reduceMatrixBy(craftMatrix, canCraftAmount * amount);
                            if (canBeCraftedTimes(inventory.getMatrix(), amount) == 0) {
                                scheduler.runTask(hmc, () -> inventory.setResult(null));
                            }
                        }
                    }

                    e.setCancelled(true);

                } else {

                    if (e.getClick().equals(ClickType.NUMBER_KEY)
                            && e.getWhoClicked().getInventory().getStorageContents()[e.getHotbarButton()] != null) {

                        e.setCancelled(true);
                        return;
                    }

                    reduceMatrixBy(craftMatrix, amount - 1);
                }

            } else {
                inventory.setResult(null);
                Message.create("modCraftingRemoved", hmc, "Crafting")
                        .addPlaceholder("%MATERIAL%", Utils.makeStringFriendly(result.getType().toString()))
                        .addPlaceholder("%AMOUNT%", amount)
                        .send(e.getWhoClicked());
            }
        }
    }

    private void reduceMatrixBy(ItemStack[] craftMatrix, int reduceBy) {
        for (ItemStack matrixItem : craftMatrix) {
            matrixItem.setAmount(matrixItem.getAmount() - reduceBy);
        }
    }

    private int canBeCraftedTimes(ItemStack[] craftMatrix, int amountPerUnit) {
        int canBeCrafted = Integer.MAX_VALUE;
        for (ItemStack matrixItem : craftMatrix) {
            if (matrixItem != null) {
                canBeCrafted = Math.min(canBeCrafted, matrixItem.getAmount() / amountPerUnit);

                if (canBeCrafted == 0) {
                    return 0;
                }
            }
        }

        if (canBeCrafted == Integer.MAX_VALUE) {
            return 0;
        }

        return canBeCrafted;
    }

    @Override
    public void loadConfig() {

        List<String> materialAmounts = hmc.getConfig().getStringList("crafting.materialAmounts");
        craftAmountMap = new HashMap<>();
        for (String pair : materialAmounts) {

            StringArgumentSeparator separator = new StringArgumentSeparator(pair);
            if (separator.meetsLength(2)) {

                Material material = Material.matchMaterial(separator.getArgument(0));
                int amount = separator.getArgumentIntMinimum(1, 1);
                if (material != null) {
                    craftAmountMap.put(material, amount);
                } else {
                    logInvalidPair(pair);
                }

            } else {
                logInvalidPair(pair);
            }
        }
    }

    private void logInvalidPair(String invalidPair) {
        Message.create("modCraftingInvalidLog", hmc)
                .addPlaceholder("%INVALID%", invalidPair)
                .log(Level.WARNING);
    }
}
