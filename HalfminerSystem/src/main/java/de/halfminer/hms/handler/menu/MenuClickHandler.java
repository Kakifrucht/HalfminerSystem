package de.halfminer.hms.handler.menu;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Implementing class handles menu clicks.
 */
public interface MenuClickHandler {

    /**
     * Called when a {@link InventoryClickEvent} was fired on a menu.
     * The given takes menu pagination into account.
     * Example: If menu is on page 3 and {@link InventoryClickEvent#getRawSlot()} returned 2,
     * rawSlot would be 2 + (3 * slotsPerPage).
     *
     * @param clickEvent click event that triggers the method invocation
     * @param rawSlot slot that keeps track of current paginated slot
     */
    void handleClick(InventoryClickEvent clickEvent, int rawSlot);
}
