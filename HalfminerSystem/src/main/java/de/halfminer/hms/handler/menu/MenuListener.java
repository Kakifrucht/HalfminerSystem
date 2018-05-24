package de.halfminer.hms.handler.menu;

import de.halfminer.hms.HalfminerClass;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;

import java.util.*;

/**
 * Listening for inventory clicks, inventory close and plugin disables, to handle currently active menus.
 */
public class MenuListener extends HalfminerClass implements Listener {

    private final Map<Inventory, MenuContainer> menuMap = new HashMap<>();


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {

        if (menuMap.containsKey(e.getClickedInventory())) {
            e.setCancelled(true);

            menuMap.get(e.getClickedInventory())
                    .getClickHandler()
                    .handleClick(e);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        menuMap.remove(e.getInventory());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        // close all menus of plugin, copy to new hashset to prevent concurrentmodificationexception
        new HashSet<>(menuMap.values())
                .stream()
                .filter(m -> m.getPlugin().equals(e.getPlugin()))
                .forEach(MenuContainer::closeMenu);
    }

    public List<MenuContainer> getActiveMenuContainers() {
        return new ArrayList<>(menuMap.values());
    }

    public void showMenu(MenuContainer menuContainer) {
        menuMap.put(menuContainer.getInventory(), menuContainer);
        menuContainer.showMenu();
    }
}
