package de.halfminer.hmc.module.sell;

import de.halfminer.hms.handler.HanStorage;
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
    private final Map<Material, Sellable> cycleSellablesLookup = new HashMap<>();


    DefaultSellCycle(long expirySeconds) {
        this.expiry = expirySeconds;
    }

    @Override
    public void addSellableToCycle(Sellable toAdd) {
        cycleSellables.add(toAdd);
        cycleSellablesLookup.put(toAdd.getMaterial(), toAdd);
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
    public Sellable getSellable(Material material) {
        return cycleSellablesLookup.get(material);
    }

    @Override
    public void storeCurrentCycle(HanStorage storage) {
        if (!isEmpty()) {

            storage.set("sellcycle", null);
            storage.set("sellcycle.expires", expiry);

            for (int i = 0; i < cycleSellables.size(); i++) {
                Sellable sellable = cycleSellables.get(i);
                String basePath = "sellcycle." + i + ".";
                storage.set(basePath + "group", sellable.getGroup().getGroupName());
                storage.set(basePath + "material", sellable.getMaterial().toString());
                storage.set(basePath + "state", sellable.getStateString());
            }
        }
    }
}
