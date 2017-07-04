package de.halfminer.hmc.module.sell;

import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.util.Pair;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link SellCycle}.
 */
class DefaultSellCycle implements SellCycle {

    private final long expiry;

    private final List<Sellable> cycleSellables = new ArrayList<>();
    private final Map<Pair<Material, Short>, Sellable> cycleSellablesLookup = new HashMap<>();


    DefaultSellCycle(long expirySeconds) {
        this.expiry = expirySeconds;
    }

    @Override
    public void addSellableToCycle(Sellable toAdd) {
        cycleSellables.add(toAdd);
        cycleSellablesLookup.put(new Pair<>(toAdd.getMaterial(), toAdd.getDurability()), toAdd);
    }

    @Override
    public long getExpiryTimestamp() {
        return expiry;
    }

    @Override
    public long getSecondsTillExpiry() {
        return Math.max(0, expiry - (System.currentTimeMillis() / 1000));
    }

    @Override
    public boolean isEmpty() {
        return cycleSellables.isEmpty();
    }

    @Override
    public List<Sellable> getSellables() {
        return cycleSellables;
    }

    @Override
    public Sellable getSellableAtSlot(int slot) {
        return slot < cycleSellables.size() && slot >= 0 ? cycleSellables.get(slot) : null;
    }

    @Override
    public Sellable getMatchingSellable(Material material, short data) {
        Pair<Material, Short> lookupPair = new Pair<>(material, data);
        if (cycleSellablesLookup.containsKey(lookupPair)) {
            return cycleSellablesLookup.get(lookupPair);
        } else if (lookupPair.getRight() >= 0) {
            lookupPair.setRight((short) -1);
            return cycleSellablesLookup.get(lookupPair);
        }
        return null;
    }

    @Override
    public void storeCurrentCycle(HanStorage storage) {
        if (!isEmpty()) {

            storage.set("sellcycle", null);
            storage.set("sellcycle.expires", expiry);

            for (int i = 0; i < cycleSellables.size(); i++) {
                Sellable sellable = cycleSellables.get(i);
                String basePath = "sellcycle." + i + ".";
                storage.set(basePath + "groupId", sellable.getGroupId());
                storage.set(basePath + "material", sellable.getMaterial().toString());
                storage.set(basePath + "durability", sellable.getDurability());
                storage.set(basePath + "state", sellable.getStateString());
            }
        }
    }
}
