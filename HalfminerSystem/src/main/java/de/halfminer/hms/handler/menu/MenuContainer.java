package de.halfminer.hms.handler.menu;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Class containing necessary menu data (calling {@link MenuCreator}, owning {@link Player},
 * menu title, menu items, pagination count) and methods to open (which will create a {@link Inventory} to
 * show it to the owning player, and close the menu. It handles pagination by itself and can show the next page
 * via {@link #showMenuPrevious()} and {@link #showMenuNext()}.
 */
public class MenuContainer {

    private static final int MENU_SLOTS_PER_ROW = 9;

    private final MenuCreator menuCreator;
    private final Plugin plugin;
    private final Player player;

    private final boolean isPaginated;
    private final String title;
    private final ItemStack[] menuItems;
    private final MenuClickHandler clickHandler;

    private final int slotsUntilPagination;
    private final int previousPageRawSlot;
    private final int nextPageRawSlot;

    private Inventory currentInventory;
    private int currentPage = 0;


    public MenuContainer(MenuCreator menuCreator, Player player,
                         String title, ItemStack[] menuItems, MenuClickHandler clickHandler) {
        this.menuCreator = menuCreator;
        this.plugin = menuCreator.getPlugin();
        this.player = player;

        // read from config
        ConfigurationSection config = HalfminerSystem.getInstance().getConfig().getConfigurationSection("handler.menu");
        this.slotsUntilPagination = config.getInt("slotsUntilPagination", 45);
        this.previousPageRawSlot = config.getInt("paginationControls.previous", 0) + slotsUntilPagination;
        this.nextPageRawSlot = config.getInt("paginationControls.next", 8) + slotsUntilPagination;

        this.isPaginated = menuItems.length > slotsUntilPagination;
        this.title = title;
        this.menuItems = menuItems;
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

    /**
     * If menu is still opened it will be closed delayed on next tick,
     * to prevent items from glitching out (during InventoryClickEvent).
     */
    public void closeMenu() {
        if (isOpened()) {
            plugin.getServer().getScheduler().runTask(plugin, player::closeInventory);
        }
    }

    void handleClick(InventoryClickEvent event) {
        if (clickHandler != null) {
            clickHandler.handleClick(event, (currentPage * slotsUntilPagination) + event.getRawSlot());
        }
    }

    boolean isPaginationSlot(int rawSlot) {
        return isPaginated && rawSlot >= slotsUntilPagination;
    }

    void showMenu() {

        int menuSize = isPaginated ? slotsUntilPagination + MENU_SLOTS_PER_ROW : menuItems.length;
        if (menuSize % MENU_SLOTS_PER_ROW != 0) {
            menuSize += MENU_SLOTS_PER_ROW - (menuSize % MENU_SLOTS_PER_ROW);
        }

        Inventory inventory = plugin.getServer().createInventory(player, menuSize, title);
        int startPoint = currentPage * slotsUntilPagination;
        for (int i = startPoint; i < getCurrentPage() * slotsUntilPagination && i < menuItems.length; i++) {
            inventory.setItem(i - startPoint, menuItems[i]);
        }

        if (isPaginated) {

            // pre fill row with glass panes
            ItemStack rowPane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
            Utils.setDisplayName(rowPane, " ");
            for (int i = slotsUntilPagination; i < slotsUntilPagination + MENU_SLOTS_PER_ROW; i++) {
                inventory.setItem(i, rowPane);
            }

            // add necessary buttons
            ItemStack paginationButtonStack = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            if (hasPreviousPage()) {
                Utils.applyLocaleToItemStack(paginationButtonStack, getPaginationMessageBuilder("hanMenuPageStackPrevious"));
                inventory.setItem(previousPageRawSlot, paginationButtonStack);
            }

            if (hasNextPage()) {
                Utils.applyLocaleToItemStack(paginationButtonStack, getPaginationMessageBuilder("hanMenuPageStackNext"));
                inventory.setItem(nextPageRawSlot, paginationButtonStack);
            }
        }

        this.currentInventory = inventory;
        player.openInventory(inventory);
    }

    private MessageBuilder getPaginationMessageBuilder(String langKey) {
        return MessageBuilder.create(langKey, HalfminerSystem.getInstance())
                .addPlaceholder("%PAGE%", getCurrentPage())
                .addPlaceholder("%PAGECOUNT%", getPageCount());
    }

    int getPreviousPageRawSlot() {
        return hasPreviousPage() ? previousPageRawSlot : -1;
    }

    int getNextPageRawSlot() {
        return hasNextPage() ? nextPageRawSlot : -1;
    }

    void showMenuPrevious() {

        if (hasPreviousPage()) {
            currentPage--;
        }

        showMenu();
    }

    void showMenuNext() {

        if (hasNextPage()) {
            currentPage++;
        }

        showMenu();
    }

    boolean isOpened() {
        return !this.currentInventory.getViewers().isEmpty();
    }

    private int getCurrentPage() {
        return currentPage + 1;
    }

    private int getPageCount() {

        if (!isPaginated) {
            return 1;
        }

        return (menuItems.length / slotsUntilPagination) + 1;
    }

    private boolean hasPreviousPage() {
        return isPaginated && currentPage > 0;
    }

    private boolean hasNextPage() {
        return isPaginated && menuItems.length > getCurrentPage() * slotsUntilPagination;
    }
}
