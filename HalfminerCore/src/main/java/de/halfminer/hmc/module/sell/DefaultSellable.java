package de.halfminer.hmc.module.sell;

import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.StringArgumentSeparator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Default implementation of {@link Sellable}.
 */
class DefaultSellable implements Sellable {

    private final SellableGroup group;
    private final Material material;
    private final short durability;
    private final String messageName;

    private final int baseUnitAmount;

    private Map<UUID, Integer> amountSoldMap;
    private int amountSoldTotal;


    DefaultSellable(SellableGroup group, Material material, short durability, String messageName, int baseUnitAmount) {

        this.group = group;
        this.material = material;
        this.durability = durability;
        this.messageName = messageName;

        this.baseUnitAmount = baseUnitAmount;

        doReset();
    }

    @Override
    public SellableGroup getGroup() {
        return group;
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

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<UUID, Integer> uuidIntegerEntry : amountSoldMap.entrySet()) {
            sb.append(uuidIntegerEntry.getKey().toString())
                    .append(" ")
                    .append(uuidIntegerEntry.getValue())
                    .append(" ");
        }

        if (sb.toString().endsWith(" ")) {
            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    @Override
    public void setState(String state) {

        doReset();

        StringArgumentSeparator separator = new StringArgumentSeparator(state);
        for (int i = 0; separator.meetsLength(i + 2); i += 2) {

            UUID uuid;
            try {
                uuid = UUID.fromString(separator.getArgument(i));
            } catch (IllegalArgumentException e) {
                // invalid format, don't recover state
                return;
            }

            int amount = separator.getArgumentInt(i + 1);
            if (amount == Integer.MIN_VALUE) {
                return;
            }

            amountSoldMap.put(uuid, amount);
        }

        computeAmountSoldTotal();
    }

    @Override
    public int getBaseUnitAmount() {
        return baseUnitAmount;
    }

    @Override
    public Map<UUID, Integer> getAmountSoldMap() {
        return amountSoldMap;
    }

    @Override
    public long getCurrentUnitAmount(Player player) {
        return getUnitAmountAndAmountUntilIncrease(player).getLeft();
    }

    @Override
    public int getAmountUntilNextIncrease(Player player) {
        return getUnitAmountAndAmountUntilIncrease(player).getRight();
    }

    private Pair<Long, Integer> getUnitAmountAndAmountUntilIncrease(Player player) {

        int amountSold = amountSoldMap.getOrDefault(player.getUniqueId(), 0);

        long currentUnitAmount = baseUnitAmount;
        int amountUntilIncrease = baseUnitAmount * group.getUnitsUntilIncrease();

        amountUntilIncrease -= amountSold;

        // update price if necessary
        while (amountUntilIncrease <= 0) {

            long newUnitAmount = Math.round((double) currentUnitAmount * group.getPriceAdjustMultiplier());
            // ensure that unit amount increases by one, at least
            if (newUnitAmount == currentUnitAmount) {
                newUnitAmount++;
            }

            currentUnitAmount = newUnitAmount;
            amountUntilIncrease += (baseUnitAmount * group.getUnitsUntilIncrease());
        }

        return new Pair<>(currentUnitAmount, amountUntilIncrease);
    }

    @Override
    public int getAmountSoldTotal() {
        return amountSoldTotal;
    }

    @Override
    public void doReset() {
        amountSoldMap = new HashMap<>();
        computeAmountSoldTotal();
    }

    @Override
    public boolean isSimiliar(Sellable sellable) {
        return sellable.getMaterial().equals(material) && sellable.getDurability() == durability;
    }

    @Override
    public void copyStateFromSellable(Sellable toCopy) {
        this.amountSoldMap = toCopy.getAmountSoldMap();
        computeAmountSoldTotal();
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

        Pair<Long, Integer> valueNextIncreasePair = getUnitAmountAndAmountUntilIncrease(hasSold);
        long unitAmount = valueNextIncreasePair.getLeft();
        int amountUntilNextIncrease = valueNextIncreasePair.getRight();

        int amountSoldPartial = amountSold;
        if (amountSoldPartial > amountUntilNextIncrease) {
            amountSoldPartial = amountUntilNextIncrease;
        }

        double revenue = amountSoldPartial / (double) unitAmount;

        // update total amount of player
        int amountSoldTotalPlayer = amountSoldPartial;
        if (amountSoldMap.containsKey(hasSold.getUniqueId())) {
            amountSoldTotalPlayer += amountSoldMap.get(hasSold.getUniqueId());
        }
        amountSoldMap.put(hasSold.getUniqueId(), amountSoldTotalPlayer);
        this.amountSoldTotal += amountSoldPartial;

        if (amountSold > amountSoldPartial) {
            // if necessary, call recursively while unitAmount changes for next call
            return revenue + getRevenue(hasSold, amountSold - amountSoldPartial);
        } else {
            return revenue;
        }
    }

    @Override
    public Map.Entry<UUID, Integer> soldMostBy() {
        Map.Entry<UUID, Integer> soldMost = null;
        for (Map.Entry<UUID, Integer> uuidIntegerEntry : amountSoldMap.entrySet()) {
            if (soldMost == null || uuidIntegerEntry.getValue() > soldMost.getValue()) {
                soldMost = uuidIntegerEntry;
            }
        }
        return soldMost;
    }

    @Override
    public String toString() {
        return messageName + " (" + material.toString() + ")";
    }

    private void computeAmountSoldTotal() {
        this.amountSoldTotal = 0;
        for (Integer amountSold : amountSoldMap.values()) {
            amountSoldTotal += amountSold;
        }
    }
}
