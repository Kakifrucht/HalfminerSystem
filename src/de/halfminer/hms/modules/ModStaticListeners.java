package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.List;

/**
 * - Removes quit message
 * - Keeps itemname of colored items in anvil
 * - Commandfilter
 *   - Disables commands in bed (teleport glitch)
 *   - Rewrites /pluginname:command to just /command
 * - Disables tab completes that are too long, defaults to help instead
 */
@SuppressWarnings("unused")
public class ModStaticListeners extends HalfminerModule implements Listener {

    @EventHandler
    public void quitNoMessage(PlayerQuitEvent e) {
        e.setQuitMessage("");
    }

    @EventHandler(ignoreCancelled = true)
    public void anvilKeepName(InventoryClickEvent e) {

        Inventory clickedInv = e.getInventory();
        if (clickedInv == null || !(e.getWhoClicked() instanceof Player)) return;

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
        if (player.hasPermission("hms.bypass.commandfilter")) return;
        if (player.isSleeping()) {
            player.sendMessage(Language.getMessagePlaceholders("modStaticListenersCommandSleep", true, "%PREFIX%", "Info"));
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
    public void tabCompleteFilter(TabCompleteEvent e) {

        String buffer = e.getBuffer();
        List<String> complete = e.getCompletions();

        /*
          Conditions breakdown (replace tab complete with /hilfe, if...):
            - Buffer is a command
            - The sender doesn't have bypass permission
            - Either..
              - There are more than 10 completions and they are commands
              - When there are 0 completions the buffer must not contain a space
                (not root command, so no need to overwrite buffer)
         */
        if (buffer.startsWith("/")
                && !e.getSender().hasPermission("hms.bypass.tabcomplete")
                && ((complete.size() > 10 && complete.get(0).startsWith("/"))
                || (complete.size() == 0 && !buffer.contains(" ")))) {

            e.getSender().sendMessage(Language.getMessagePlaceholders("modStaticListenersTabHelp", true,
                    "%PREFIX%", "Info"));
            e.setCompletions(Collections.singletonList("/hilfe"));
        }
    }
}
