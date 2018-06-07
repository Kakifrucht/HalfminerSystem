package de.halfminer.hmc.module.sell;

/**
 * Every {@link Sellable} has a {@link SellableGroup}, which it uses to determine how many
 * {@link Sellable Sellable's} in this group get to be in a {@link SellCycle} and other cycle based sell data.
 */
interface SellableGroup {

    String getGroupName();

    int getAmountPerCycle();

    int getUnitsUntilIncrease();

    double getPriceAdjustMultiplier();
}
