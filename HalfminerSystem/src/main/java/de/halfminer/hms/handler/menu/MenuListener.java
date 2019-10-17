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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Listening for inventory clicks, inventory close and plugin disables, to handle currently active menus.
 */
public class MenuListener extends HalfminerClass implements Disableable, Sweepable, Listener {

    private final Map<Player, MenuContainer> menuMap = new WeakHashMap<>();


    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {

        if (e.getWhoClicked() instanceof Player) {

            Player whoClicked = (Player) e.getWhoClicked();
            MenuContainer menuContainer = getPlayerMenuContainer(whoClicked);
            if (menuContainer != null) {

                e.setCancelled(true);

                if (menuContainer.isPaginationSlot(e.getRawSlot())) {

                    if (e.getRawSlot() == menuContainer.getPreviousPageRawSlot()) {
                        menuContainer.showMenuPrevious();
                    } else if (e.getRawSlot() == menuContainer.getNextPageRawSlot()) {
                        menuContainer.showMenuNext();
                    }

                    return;
                }

                menuContainer.handleClick(e);
            }
        }
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        menuMap.remove(e.getPlayer());
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        // close all menus belonging to disabled plugin
        menuMap.values().stream()
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
            } else {
                menuMap.remove(player);
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
        menuMap.values().forEach(menu -> menu.getPlayer().closeInventory());
    }

    @Override
    public void sweep() {
        menuMap.values().removeIf(menuContainer -> !menuContainer.isOpened());
    }
}
