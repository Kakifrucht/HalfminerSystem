package de.halfminer.hmc.module.sell;

import de.halfminer.hms.handler.HanStorage;
import org.bukkit.Material;

import java.util.List;

/**
 * Class managing the current sell cycle, depending on {@link SellableMap} to get its {@link Sellable data} from.
 */
public interface SellCycle {

    void addSellableToCycle(Sellable toAdd);

    long getExpiryTimestamp();

    long getSecondsTillExpiry();

    boolean isEmpty();

    List<Sellable> getSellables();

    Sellable getSellableAtSlot(int slot);

    Sellable getMatchingSellable(Material material, short data);

    @SuppressWarnings("SameParameterValue")
    void storeCurrentCycle(HanStorage storage);
}
