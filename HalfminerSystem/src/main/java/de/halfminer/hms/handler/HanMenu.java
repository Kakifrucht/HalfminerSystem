package de.halfminer.hms.handler;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.handler.menu.MenuClickHandler;
import de.halfminer.hms.handler.menu.MenuContainer;
import de.halfminer.hms.handler.menu.MenuCreator;
import de.halfminer.hms.handler.menu.MenuListener;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * - Opens inventory based menus, classes must implement {@link MenuCreator} interface to create them
 *   - Get all currently opened menus that were created by a given {@link MenuCreator}
 * - Prevents entering/removing items from menu
 * - Classes can optionally pass a {@link MenuClickHandler} to handle inventory interaction
 * - Automatically adds pagination, previous/next page buttons will be added if necessary
 */
@SuppressWarnings("unused")
public class HanMenu extends HalfminerClass {

    private final MenuListener menuListener;


    public HanMenu() {
        this.menuListener = new MenuListener();
    }

    /**
     * Open a menu wrapped in a {@link MenuContainer} for a given player.
     * The menu will be protected from item theft.
     * @param menuContainer that contains the necessary menu data
     */
    public void openMenu(MenuContainer menuContainer) {
        menuListener.showMenu(menuContainer);
    }

    /**
     * Calls {@link MenuContainer#closeMenu()} of a given player, only if a menu is opened.
     *
     * @param player to close the active menu from
     */
    public void closeMenu(Player player) {

        MenuContainer menuContainer = menuListener.getPlayerMenuContainer(player);
        if (menuContainer != null) {
            menuContainer.closeMenu();
        }
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
