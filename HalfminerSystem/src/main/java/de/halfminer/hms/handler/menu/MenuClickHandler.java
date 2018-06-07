package de.halfminer.hms.handler.menu;

import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Implementing class handles menu clicks.
 */
public interface MenuClickHandler {

    /**
     * Called when a {@link InventoryClickEvent} was fired while a menu was opened.
     * The given rawSlot takes menu pagination into account, whereas {@link InventoryClickEvent#getRawSlot()} does not.
     * Example: If menu is on page 3 and {@link InventoryClickEvent#getRawSlot()} returned 2,
     * rawSlot parameter would be 2 + ((3 - 1) * slotsPerPage).
     *
     * @param clickEvent click event that triggers the method invocation
     * @param rawSlot slot that keeps track of current paginated slot
     */
    void handleClick(InventoryClickEvent clickEvent, int rawSlot);
}
