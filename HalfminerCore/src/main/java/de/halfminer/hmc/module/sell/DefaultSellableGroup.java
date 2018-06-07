package de.halfminer.hmc.module.sell;

/**
 * Default implementation of {@link SellableGroup}.
 */
class DefaultSellableGroup implements SellableGroup {

    private final String groupName;

    private final int amountPerCycle;
    private final int unitsUntilIncrease;
    private final double priceAdjustMultiplier;


    DefaultSellableGroup(String groupName, int amountPerCycle, int unitsUntilIncrease, double priceAdjustMultiplier) {

        this.groupName = groupName;
        this.amountPerCycle = amountPerCycle;
        this.unitsUntilIncrease = unitsUntilIncrease;
        this.priceAdjustMultiplier = priceAdjustMultiplier;
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public int getAmountPerCycle() {
        return amountPerCycle;
    }

    @Override
    public int getUnitsUntilIncrease() {
        return unitsUntilIncrease;
    }

    @Override
    public double getPriceAdjustMultiplier() {
        return priceAdjustMultiplier;
    }

    @Override
    public int hashCode() {
        return groupName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj
                || (obj instanceof DefaultSellableGroup && ((DefaultSellableGroup) obj).groupName.equals(groupName));
    }
}
