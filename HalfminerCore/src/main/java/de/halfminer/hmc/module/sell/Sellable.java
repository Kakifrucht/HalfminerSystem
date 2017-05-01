package de.halfminer.hmc.module.sell;

import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

/**
 * Class encapsulating an item that is up for sale, and it's necessary metadata, managed by {@link SellableMap}.
 * Also handles it's current unit price and returns revenue from given sell amount.
 */
public class Sellable {

    // cyclic dependency alert
    private final SellableMap sellableMap;

    // current configuration dependant on SellableMap's loaded state
    private final int groupId;
    private int idInGroup;

    private final Material material;
    private final short durability;
    private final String messageName;
    private final int baseUnitAmount;

    private int currentUnitAmount;
    private int amountUntilNextIncrease;


    Sellable(SellableMap sellableMap, int groupId, int idInGroup,
             Material material, short durability, String messageName, int baseUnitAmount) {

        this.sellableMap = sellableMap;

        this.groupId = groupId;
        this.idInGroup = idInGroup;

        this.material = material;
        this.durability = durability;
        this.messageName = messageName;

        this.baseUnitAmount = baseUnitAmount;
        doRandomReset();
    }

    int getGroupId() {
        return groupId;
    }

    int getIdInGroup() {
        return idInGroup;
    }

    Material getMaterial() {
        return material;
    }

    short getDurability() {
        return durability;
    }

    String getStateString() {
        return currentUnitAmount + " " + amountUntilNextIncrease;
    }

    void setState(String state) {
        StringArgumentSeparator separator = new StringArgumentSeparator(state);
        currentUnitAmount = separator.getArgumentInt(0);
        amountUntilNextIncrease = separator.getArgumentInt(1);
    }

    void doRandomReset() {

        Random rnd = new Random();
        double priceVarianceFactor = sellableMap.getPriceVarianceFactor();

        double factorRandomized = rnd.nextDouble() * priceVarianceFactor;
        if (rnd.nextBoolean()) {
            factorRandomized = -factorRandomized;
        }

        priceVarianceFactor = 1.0d + factorRandomized;
        this.currentUnitAmount = (int) (priceVarianceFactor * (double) baseUnitAmount);
        this.amountUntilNextIncrease = currentUnitAmount * sellableMap.getUnitsUntilIncrease();
    }

    public ItemStack getItemStack() {
        return new ItemStack(material, 1, durability);
    }

    public String getMessageName() {
        return messageName;
    }

    public boolean isMatchingStack(ItemStack itemStack) {
        return itemStack != null
                && material.equals(itemStack.getType())
                && durability == itemStack.getDurability()
                && !itemStack.hasItemMeta();
    }

    public int getCurrentUnitAmount() {
        return currentUnitAmount;
    }

    public int getAmountUntilNextIncrease() {
        return amountUntilNextIncrease;
    }

    public double getRevenue(int amountSold) {

        double revenue = amountSold / (double) currentUnitAmount;
        amountUntilNextIncrease -= amountSold;
        if (amountUntilNextIncrease < 0) {
            currentUnitAmount *= sellableMap.getPriceAdjustMultiplier();
            amountUntilNextIncrease += (currentUnitAmount * sellableMap.getUnitsUntilIncrease());
        }
        return revenue;
    }
}
