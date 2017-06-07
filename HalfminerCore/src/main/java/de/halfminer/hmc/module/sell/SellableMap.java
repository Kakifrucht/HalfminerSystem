package de.halfminer.hmc.module.sell;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Class managing {@link Sellable Sellables} read from config and the current cycle they are in,
 * aswell as storing/reading current cycle from cold storage and kicking off new cycles on demand.
 */
public interface SellableMap {

    double getPriceAdjustMultiplier();

    double getPriceVarianceFactor();

    int getUnitsUntilIncrease();

    long getCycleTimeLeft();

    List<Sellable> getCycleSellables();

    Sellable getSellableAtSlot(int slotId);

    Sellable getSellableFromItemStack(ItemStack item);

    void configReloaded(ConfigurationSection sellableSection,
                               int cycleTimeSecondsMax, int cycleTimeSecondsMin, int cycleMinPlayerCount,
                               double priceAdjustMultiplier, double priceVarianceFactor, int unitsUntilIncrease);

    void storeCurrentCycle();

    void forceNewCycle();
}
