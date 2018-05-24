package de.halfminer.hms.handler.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;

/**
 * Class containing necessary menu data, and convenience methods to open and close the menu.
 */
public class MenuContainer {

    private final MenuCreator menuCreator;
    private final Plugin plugin;
    private final Player player;
    private final Inventory inventory;
    private final MenuClickHandler clickHandler;


    public MenuContainer(MenuCreator menuCreator, Player player, Inventory inventory, MenuClickHandler clickHandler) {
        this.menuCreator = menuCreator;
        this.plugin = menuCreator.getPlugin();
        this.player = player;
        this.inventory = inventory;
        this.clickHandler = clickHandler;
    }

    public MenuCreator getMenuCreator() {
        return menuCreator;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public Inventory getInventory() {
        return inventory;
    }

    MenuClickHandler getClickHandler() {
        return clickHandler;
    }

    void showMenu() {
        player.openInventory(inventory);
    }

    public void closeMenu() {
        player.closeInventory();
    }
}
