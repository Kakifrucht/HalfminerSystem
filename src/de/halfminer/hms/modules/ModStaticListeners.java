package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Contains functions, that are static and have no fitting module
 * - Removes leave messages
 * - Blocks certain items from being traded with villagers/merchants
 * - Override spigot teleport safety
 * - Denies chatting when globalmute is enabled
 * - Capsfilter that automatically lowercases capitalised messages
 * - Disables commands while in bed
 * - Disables command in format '/pluginname:command'
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

    @EventHandler
    public void chatFilter(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (storage.getBoolean("sys.globalmute") && !p.hasPermission("hms.bypass.globalmute")) {
            p.sendMessage(Language.getMessagePlaceholders("commandChatGlobalmuteDenied", true,
                    "%PREFIX%", "Globalmute"));
            e.setCancelled(true);
        } else {
            String message = e.getMessage();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_HAT, 1.0f, 2.0f);
            if (p.hasPermission("hms.bypass.capsfilter") || message.length() < 4) return;

            int amountUppercase = 0;
            for (Character check : message.toCharArray()) if (Character.isUpperCase(check)) amountUppercase++;
            if (amountUppercase > (message.length() / 2)) e.setMessage(message.toLowerCase());
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
}
