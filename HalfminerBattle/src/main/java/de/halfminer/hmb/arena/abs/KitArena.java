package de.halfminer.hmb.arena.abs;

import org.bukkit.inventory.ItemStack;

/**
 * Setter and getter for arenas using kits, like {@link AbstractKitArena}
 */
public interface KitArena extends Arena {

    /**
     * @return the arenas current kit, or null if none defined
     */
    ItemStack[] getKit();

    /**
     * @param kit to set for the arena
     */
    void setKit(ItemStack[] kit);
}
