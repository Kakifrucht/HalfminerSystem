package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;

/**
 * - Removes quit message
 * - Disables some deals in villager trades
 * - Commandfilter
 *   - Disables commands in bed (teleport glitch)
 *   - Disables /pluginname:command for users (to improve commandblocks)
 * - Disables tab completes that are too long, defaults to help instead
 */
@SuppressWarnings("unused")
public class ModStaticListeners extends HalfminerModule implements Listener {

    @EventHandler
    public void quitNoMessage(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler(ignoreCancelled = true)
    public void merchantBlock(InventoryClickEvent e) {
        Inventory clicked = e.getInventory();
        if (clicked != null
                && clicked.getType() == InventoryType.MERCHANT
                && !e.getWhoClicked().hasPermission("hms.bypass.merchant")) {
            ItemStack item = e.getCurrentItem();
            if (item != null && item.getType() == Material.WRITTEN_BOOK) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void commandFilter(PlayerCommandPreprocessEvent e) {

        Player player = e.getPlayer();
        if (player.hasPermission("hms.bypass.commandfilter")) return;
        if (player.isSleeping()) {
            player.sendMessage(Language.getMessagePlaceholders("modStaticListenersCommandSleep", true, "%PREFIX%", "Info"));
            e.setCancelled(true);
        } else {
            // Deny commands, that contain ':', which could be used to bypass certain filters via /pluginname:command
            for (Character check : e.getMessage().toLowerCase().toCharArray()) {
                if (check.equals(' ')) return;
                if (check.equals(':')) {
                    e.getPlayer().sendMessage(Language.getMessagePlaceholders("noPermission", true, "%PREFIX%", "Info"));
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void tabCompleteFilter(TabCompleteEvent e) {

        if (e.getCompletions().size() > 10 && !e.getSender().hasPermission("hms.bypass.tabcomplete")) {
            e.getSender().sendMessage(Language.getMessagePlaceholders("modStaticListenersTabHelp", true,
                    "%PREFIX%", "Info"));
            e.setCompletions(Collections.singletonList("/hilfe"));
        }
    }
}
