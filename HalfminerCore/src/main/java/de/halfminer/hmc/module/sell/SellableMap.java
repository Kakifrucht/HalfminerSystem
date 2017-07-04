package de.halfminer.hmc.module.sell;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

/**
 * Class managing {@link Sellable Sellables} read from config and the current {@link SellCycle} they are being used in,
 * aswell as reading current cycle from cold storage and kicking off/constructing new {@link SellCycle}'s.
 */
public interface SellableMap {

    double getPriceAdjustMultiplier();

    double getPriceVarianceFactor();

    int getUnitsUntilIncrease();

    boolean hasCycle();

    SellCycle getCurrentCycle();

    Sellable getSellableAtSlot(int slotId);

    Sellable getSellableFromItemStack(ItemStack item);

    void configReloaded(ConfigurationSection sellableSection,
                               int cycleTimeSecondsMax, int cycleTimeSecondsMin, int cycleMinPlayerCount,
                               double priceAdjustMultiplier, double priceVarianceFactor, int unitsUntilIncrease);

    void storeCurrentCycle();

    void createNewCycle();
}
