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
import java.util.List;

/**
 * - Removes quit message
 * - Disables some deals in villager trades
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
