package de.halfminer.hms.handler.menu;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Sweepable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Listening for inventory clicks, inventory close and plugin disables, to handle currently active menus.
 */
public class MenuListener extends HalfminerClass implements Disableable, Sweepable, Listener {

    private final Map<Player, MenuContainer> menuMap = new HashMap<>();


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {

        if (e.getWhoClicked() instanceof Player) {

            Player whoClicked = (Player) e.getWhoClicked();
            if (menuMap.containsKey(whoClicked)) {

                MenuContainer container = menuMap.get(whoClicked);
                if (container.isOpened()) {

                    e.setCancelled(true);

                    boolean handleClick = true;
                    if (container.isPaginated()) {

                        if (e.getRawSlot() == container.getPreviousPageRawSlot()) {
                            container.showMenuPrevious();
                            handleClick = false;
                        } else if (e.getRawSlot() == container.getNextPageRawSlot()) {
                            container.showMenuNext();
                            handleClick = false;
                        }
                    }

                    // don't handle click if pagination buttons were pressed
                    if (handleClick) {
                        container.handleClick(e);
                    }

                } else {
                    menuMap.remove(whoClicked);
                }
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        menuMap.remove(e.getPlayer());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        // close all menus of plugin, copy to new hashset to prevent concurrentmodificationexception
        new HashSet<>(menuMap.values())
                .stream()
                .filter(m -> m.getPlugin().equals(e.getPlugin()) && m.isOpened())
                .forEach(menuContainer -> menuContainer.getPlayer().closeInventory());
    }

    public List<MenuContainer> getActiveMenuContainers() {
        return menuMap
                .values()
                .stream()
                .filter(MenuContainer::isOpened)
                .collect(Collectors.toList());
    }

    public MenuContainer getPlayerMenuContainer(Player player) {

        if (menuMap.containsKey(player)) {
            MenuContainer menuContainer = menuMap.get(player);
            if (menuContainer.isOpened()) {
                return menuContainer;
            }
        }

        return null;
    }

    public void showMenu(MenuContainer menuContainer) {
        menuMap.put(menuContainer.getPlayer(), menuContainer);
        menuContainer.showMenu();
    }

    @Override
    public void onDisable() {
        new HashSet<>(menuMap.values()).forEach(MenuContainer::closeMenu);
    }

    @Override
    public void sweep() {
        menuMap.values().removeIf(menuContainer -> !menuContainer.isOpened());
    }
}
