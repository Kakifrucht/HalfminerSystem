package de.halfminer.hmc.module.sell;

import de.halfminer.hmc.CoreClass;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Default implementation of {@link Sellable}.
 */
public class DefaultSellable extends CoreClass implements Sellable {

    private final SellableMap sellableMap;

    private final int groupId;
    private final Material material;
    private final short durability;
    private final String messageName;

    private int baseUnitAmount;

    private Map<UUID, Integer> amountSoldBy;
    private int currentUnitAmount;
    private int amountUntilNextIncrease;
    private int amountSoldTotal;


    DefaultSellable(SellableMap sellableMap, int groupId,
             Material material, short durability, String messageName, int baseUnitAmount) {
        super(false);

        this.sellableMap = sellableMap;

        this.groupId = groupId;

        this.material = material;
        this.durability = durability;
        this.messageName = messageName;

        this.baseUnitAmount = baseUnitAmount;
        doRandomReset();
    }

    @Override
    public int getGroupId() {
        return groupId;
    }

    @Override
    public Material getMaterial() {
        return material;
    }

    @Override
    public short getDurability() {
        return durability;
    }

    @Override
    public String getStateString() {
        return currentUnitAmount + " " + amountUntilNextIncrease + " " + amountSoldTotal;
    }

    @Override
    public void setState(String state) {
        StringArgumentSeparator separator = new StringArgumentSeparator(state);
        currentUnitAmount = separator.getArgumentInt(0);
        amountUntilNextIncrease = separator.getArgumentInt(1);
        amountSoldTotal = separator.getArgumentInt(2);
    }

    @Override
    public int getBaseUnitAmount() {
        return baseUnitAmount;
    }

    @Override
    public Map<UUID, Integer> getAmountSoldBy() {
        return null;
    }

    @Override
    public int getCurrentUnitAmount() {
        return currentUnitAmount;
    }

    @Override
    public int getAmountUntilNextIncrease() {
        return amountUntilNextIncrease;
    }

    @Override
    public int getAmountSoldTotal() {
        return amountSoldTotal;
    }

    @Override
    public void doRandomReset() {

        Random rnd = new Random();
        double priceVarianceFactor = sellableMap.getPriceVarianceFactor();

        double factorRandomized = rnd.nextDouble() * priceVarianceFactor;
        if (rnd.nextBoolean()) {
            factorRandomized = -factorRandomized;
        }

        priceVarianceFactor = 1.0d + factorRandomized;
        this.currentUnitAmount = (int) Math.round(priceVarianceFactor * (double) baseUnitAmount);
        this.amountUntilNextIncrease = currentUnitAmount * sellableMap.getUnitsUntilIncrease();
        amountSoldTotal = 0;
        amountSoldBy = new HashMap<>();
    }

    @Override
    public boolean isSimiliar(Sellable sellable) {
        return sellable.getMaterial().equals(material) && sellable.getDurability() == durability;
    }

    @Override
    public void copyStateFromSellable(Sellable toCopy) {
        this.amountSoldBy = toCopy.getAmountSoldBy();
        this.currentUnitAmount = toCopy.getCurrentUnitAmount();
        this.amountUntilNextIncrease = toCopy.getAmountUntilNextIncrease();
        this.amountSoldTotal = toCopy.getAmountSoldTotal();
    }

    @Override
    public ItemStack getItemStack() {
        return new ItemStack(material, 1, (short) Math.max(durability, 0));
    }

    @Override
    public String getMessageName() {
        return messageName;
    }

    @Override
    public boolean isMatchingStack(ItemStack itemStack) {
        return itemStack != null
                && material.equals(itemStack.getType())
                && (durability < 0 || durability == itemStack.getDurability())
                && !itemStack.hasItemMeta();
    }

    @Override
    public double getRevenue(Player hasSold, int amountSold) {

        double revenue = amountSold / (double) currentUnitAmount;

        // update price if necessary
        amountUntilNextIncrease -= amountSold;
        amountSoldTotal += amountSold;
        while (amountUntilNextIncrease <= 0) {

            int newUnitAmount = (int) Math.round((double) currentUnitAmount * sellableMap.getPriceAdjustMultiplier());
            if (newUnitAmount == currentUnitAmount) {
                newUnitAmount++;
            }

            currentUnitAmount = newUnitAmount;
            amountUntilNextIncrease += (currentUnitAmount * sellableMap.getUnitsUntilIncrease());

            hasSold.playSound(hasSold.getLocation(), Sound.BLOCK_NOTE_HARP, 1.0f, 1.2f);

            MessageBuilder mb = MessageBuilder.create("modSellAmountIncreased", hmc, "Sell")
                    .addPlaceholderReplace("%NAME%", messageName)
                    .addPlaceholderReplace("%NEWAMOUNT%", String.valueOf(currentUnitAmount));
            mb.sendMessage(hasSold);
            mb.logMessage(Level.INFO);
        }

        // update non persistent storage that holds information who sold how much of this sellable
        if (amountSoldBy.containsKey(hasSold.getUniqueId())) {
            int newAmount = amountSoldBy.get(hasSold.getUniqueId()) + amountSold;
            amountSoldBy.put(hasSold.getUniqueId(), newAmount);
        } else {
            amountSoldBy.put(hasSold.getUniqueId(), amountSold);
        }

        return revenue;
    }

    @Override
    public Map.Entry<UUID, Integer> soldMostBy() {
        Map.Entry<UUID, Integer> soldMost = null;
        for (Map.Entry<UUID, Integer> uuidIntegerEntry : amountSoldBy.entrySet()) {
            if (soldMost == null || uuidIntegerEntry.getValue() > soldMost.getValue()) {
                soldMost = uuidIntegerEntry;
            }
        }
        return soldMost;
    }

    @Override
    public String toString() {
        return messageName + " (" + material.toString() + ") - " + currentUnitAmount + "/" + amountUntilNextIncrease;
    }
}
