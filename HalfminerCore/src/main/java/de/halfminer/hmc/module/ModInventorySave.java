package de.halfminer.hmc.module;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * - Only drop non enchanted items on death if enabled
 * - Toggle EXP drop
 */
@SuppressWarnings("unused")
public class ModInventorySave extends HalfminerModule implements Listener {

    private boolean isEnabled;
    private boolean keepLevel;


    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathKeepEnchantedItems(PlayerDeathEvent e) {

        Player player = e.getEntity();

        // if player has inventory bypass permission and keepInventory is not set
        if (!isEnabled
                || (player.hasPermission("hmc.bypass.inventorysave") && e.getKeepInventory())) {
            return;
        }

        boolean keepInventoryGameruleEnabled = e.getKeepInventory();

        e.setKeepInventory(true);
        e.setKeepLevel(keepLevel);

        ItemStack[] inventory = player.getInventory().getContents();
        Location playerLocation = player.getLocation();
        for (int i = 0; i < inventory.length; i++) {
            ItemStack itemStack = inventory[i];
            if (itemStack != null
                    && (!itemStack.hasItemMeta()
                    || !itemStack.getItemMeta().hasEnchants())) {

                playerLocation.getWorld().dropItem(playerLocation, itemStack);
                inventory[i] = null;
            }
        }

        player.getInventory().setContents(inventory);

        if (!e.getKeepLevel()) {
            e.setDroppedExp(Math.min(player.getLevel() * 7, 100));
            player.setLevel(0);
            player.setExp(0.0f);
        } else if (!keepInventoryGameruleEnabled) {
            e.setDroppedExp(0);
        }
    }

    @Override
    public void loadConfig() {
        isEnabled = hmc.getConfig().getBoolean("inventorySave.enable", false);
        keepLevel = hmc.getConfig().getBoolean("inventorySave.keepLevel", false);
    }
}
