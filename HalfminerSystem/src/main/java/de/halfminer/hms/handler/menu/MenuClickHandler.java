package de.halfminer.hms.handler.menu;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Implementing class handles inventory clicks.
 */
public interface MenuClickHandler {

    void handleClick(InventoryClickEvent clickEvent);
}
