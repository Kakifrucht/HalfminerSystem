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
 * Class encapsulating an item that is up for sale, and it's necessary metadata, managed by {@link SellableMap}.
 * Also handles it's current unit price and returns revenue from given sell amount.
 */
public class Sellable extends CoreClass {

    // cyclic dependency alert
    private final SellableMap sellableMap;

    private final int groupId;
    private final Material material;
    private final short durability;
    private final String messageName;

    private int baseUnitAmount;

    private Map<UUID, Integer> amountSoldBy = new HashMap<>();
    private int currentUnitAmount;
    private int amountUntilNextIncrease;


    Sellable(SellableMap sellableMap, int groupId,
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

    int getGroupId() {
        return groupId;
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
        this.currentUnitAmount = (int) Math.round(priceVarianceFactor * (double) baseUnitAmount);
        this.amountUntilNextIncrease = currentUnitAmount * sellableMap.getUnitsUntilIncrease();
    }

    boolean isSimiliar(Sellable sellable) {
        return sellable.getMaterial().equals(material) && sellable.getDurability() == durability;
    }

    void copyStateFromSellable(Sellable toCopy) {
        this.baseUnitAmount = toCopy.getBaseUnitAmount();
        this.currentUnitAmount = toCopy.getCurrentUnitAmount();
        this.amountUntilNextIncrease = toCopy.getAmountUntilNextIncrease();
    }

    public ItemStack getItemStack() {
        return new ItemStack(material, 1, (short) Math.max(durability, 0));
    }

    public String getMessageName() {
        return messageName;
    }

    /**
     * Stack is matching if it is the same Material, has the same durability or any durability if this Sellable's
     * durability is lower than 0 and has no ItemMeta.
     *
     * @param itemStack item to compare
     * @return true if stacks match, false else
     */
    public boolean isMatchingStack(ItemStack itemStack) {
        return itemStack != null
                && material.equals(itemStack.getType())
                && (durability < 0 || durability == itemStack.getDurability())
                && !itemStack.hasItemMeta();
    }

    public int getCurrentUnitAmount() {
        return currentUnitAmount;
    }

    public int getBaseUnitAmount() {
        return baseUnitAmount;
    }

    public int getAmountUntilNextIncrease() {
        return amountUntilNextIncrease;
    }

    public double getRevenue(Player hasSold, int amountSold) {

        double revenue = amountSold / (double) currentUnitAmount;

        // update price if necessary
        amountUntilNextIncrease -= amountSold;
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

    /**
     * Gets the player and amount who sold most. This information is not persistent across restarts.
     *
     * @return {@link Map.Entry}, where key is {@link UUID} of player who sold most and value the amount as {@link Integer}
     */
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
