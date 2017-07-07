package de.halfminer.hmc.module;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

/**
 * - Only drop non enchanted items on death if enabled
 *   - If an item has enchantment "Curse of Binding" and is equipped, move to inventory upon death
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

        PlayerInventory inventory = player.getInventory();
        Location playerLocation = player.getLocation();

        // drop non enchanted items
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack itemStack = contents[i];
            if (itemStack != null && !isEnchantedStack(itemStack)) {
                playerLocation.getWorld().dropItem(playerLocation, itemStack);
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);

        // move equipped and cursed items into inventory if possible
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack itemStack = armor[i];
            if (isEnchantedStack(itemStack)
                    && itemStack.getItemMeta().getEnchants().containsKey(Enchantment.BINDING_CURSE)) {

                Map<Integer, ItemStack> notAdded = inventory.addItem(itemStack);
                armor[i] = notAdded.isEmpty() ? null : notAdded.get(0);
            }
        }
        player.getInventory().setArmorContents(armor);

        if (!e.getKeepLevel()) {
            e.setDroppedExp(Math.min(player.getLevel() * 7, 100));
            player.setLevel(0);
            player.setExp(0.0f);
        } else if (!keepInventoryGameruleEnabled) {
            e.setDroppedExp(0);
        }
    }

    private boolean isEnchantedStack(ItemStack itemStack) {
        return itemStack != null
                && itemStack.hasItemMeta()
                && itemStack.getItemMeta().hasEnchants();
    }

    @Override
    public void loadConfig() {
        isEnabled = hmc.getConfig().getBoolean("inventorySave.enable", false);
        keepLevel = hmc.getConfig().getBoolean("inventorySave.keepLevel", false);
    }
}
