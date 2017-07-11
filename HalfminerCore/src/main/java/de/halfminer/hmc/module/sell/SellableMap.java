package de.halfminer.hmc.module.sell;

import org.bukkit.inventory.ItemStack;

/**
 * Class managing {@link Sellable Sellables} read from config and the current {@link SellCycle} they are being used in,
 * aswell as reading current cycle from cold storage and kicking off/constructing new {@link SellCycle}'s.
 */
public interface SellableMap {

    boolean hasCycle();

    SellCycle getCurrentCycle();

    Sellable getSellableAtSlot(int slotId);

    Sellable getSellableFromItemStack(ItemStack item);

    void storeCurrentCycle();

    void createNewCycle();
}
