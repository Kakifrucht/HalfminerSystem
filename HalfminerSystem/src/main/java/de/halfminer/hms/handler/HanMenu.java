package de.halfminer.hms.handler;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.handler.menu.MenuClickHandler;
import de.halfminer.hms.handler.menu.MenuContainer;
import de.halfminer.hms.handler.menu.MenuCreator;
import de.halfminer.hms.handler.menu.MenuListener;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * - Opens inventory based menus, classes must implement {@link MenuCreator} interface to create them
 *   - Get all currently opened menus that were created by a given {@link MenuCreator}
 * - Prevents removing items from said menu
 * - Classes implement {@link MenuClickHandler} to handle inventory interaction
 */
@SuppressWarnings("unused")
public class HanMenu extends HalfminerClass {

    private final MenuListener menuListener;


    public HanMenu() {
        this.menuListener = new MenuListener();
    }

    public void openMenu(MenuCreator menuCreator, Player player, Inventory menu, MenuClickHandler clickHandler) {
        menuListener.showMenu(new MenuContainer(menuCreator, player, menu, clickHandler));
    }

    public void closeMenu(Player player) {
        // need to close with delay to prevent glitching out items from menu
        scheduler.runTask(hms, player::closeInventory);
    }

    public List<Player> getViewingPlayers(MenuCreator menuCreator) {
        return getMatchingContainers(menuCreator)
                .stream()
                .map(MenuContainer::getPlayer)
                .collect(Collectors.toList());
    }

    public void closeAllMenus(MenuCreator menuCreator) {
        getMatchingContainers(menuCreator).forEach(MenuContainer::closeMenu);
    }

    private List<MenuContainer> getMatchingContainers(MenuCreator menuCreator) {
        return menuListener.getActiveMenuContainers()
                .stream()
                .filter(menuContainer -> menuContainer.getMenuCreator().equals(menuCreator))
                .collect(Collectors.toList());
    }
}
