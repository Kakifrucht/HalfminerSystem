package de.halfminer.hmc.module;

import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * - Removes player quit message
 * - Keeps itemname of colored items in anvil
 * - Commandfilter
 *   - Disables commands in bed (prevent teleport glitches)
 *   - Rewrites /pluginname:command to just /command
 * - Denies conversion of mobspawners with spawneggs
 */
@SuppressWarnings("unused")
public class ModStaticListeners extends HalfminerModule implements Listener {

    private static final String PREFIX = "Info";


    @EventHandler
    public void quitNoMessage(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler(ignoreCancelled = true)
    public void anvilKeepName(InventoryClickEvent e) {

        Inventory clickedInv = e.getInventory();
        if (!(e.getWhoClicked() instanceof Player)) return;

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem != null
                && !clickedItem.getType().equals(Material.AIR)
                && clickedInv.getType() == InventoryType.ANVIL
                && e.getRawSlot() == 2) {

            ItemMeta originalMeta = clickedInv.getItem(0).getItemMeta();

            if (originalMeta.hasDisplayName()
                    && originalMeta.getDisplayName().contains("" + ChatColor.COLOR_CHAR)) {

                Utils.setDisplayName(clickedItem, originalMeta.getDisplayName());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void commandFilter(PlayerCommandPreprocessEvent e) {

        Player player = e.getPlayer();
        if (player.hasPermission("hmc.bypass.commandfilter")) return;
        if (player.isSleeping()) {
            MessageBuilder.create("modStaticListenersCommandSleep", hmc, PREFIX).sendMessage(player);
            e.setCancelled(true);
        } else {

            String cmd = e.getMessage();
            // Rewrite commands that contain ':', which could be used to bypass certain filters via /pluginname:command
            for (Character check : cmd.toCharArray()) {
                if (check.equals(' ')) return;
                if (check.equals(':')) {
                    player.chat("/" + cmd.substring(cmd.indexOf(':') + 1));
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerInteract(PlayerInteractEvent e) {

        Player player = e.getPlayer();

        if (e.getClickedBlock() != null
                && e.getClickedBlock().getType().equals(Material.SPAWNER)
                && e.getItem() != null
                && e.getItem().getType().toString().endsWith("_MONSTER_EGG")
                && !player.hasPermission("hmc.bypass.spawnerconvert")) {

            MessageBuilder.create("modStaticListenersSpawnerConvert", hmc, PREFIX).sendMessage(player);
            e.setCancelled(true);
        }
    }
}
