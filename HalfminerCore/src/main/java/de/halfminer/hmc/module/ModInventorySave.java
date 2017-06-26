package de.halfminer.hmc.module;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * - Only drop non enchanted items on death if enabled
 */
@SuppressWarnings("unused")
public class ModInventorySave extends HalfminerModule implements Listener {

    private boolean isEnabled;

    @EventHandler
    public void onDeathKeepEnchantedItems(PlayerDeathEvent e) {

        if (!isEnabled) {
            return;
        }

        e.setKeepInventory(true);

        ItemStack[] inventory = e.getEntity().getInventory().getContents();
        Location playerLocation = e.getEntity().getLocation();
        for (int i = 0; i < inventory.length; i++) {
            ItemStack itemStack = inventory[i];
            if (itemStack != null
                    && (!itemStack.hasItemMeta()
                    || !itemStack.getItemMeta().hasEnchants())) {

                playerLocation.getWorld().dropItem(playerLocation, itemStack);
                inventory[i] = null;
            }
        }
        e.getEntity().getInventory().setContents(inventory);
    }

    @Override
    public void loadConfig() {
        isEnabled = hmc.getConfig().getBoolean("inventorySave.enable", true);
    }
}
