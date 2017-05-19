package de.halfminer.hmc.module.sell;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fired by {@link SellableMap} whenever a new cycle starts.
 */
public class SellCycleRefreshEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final long timeUntilNextCycle;
    private final Sellable sellableSoldMost;

    SellCycleRefreshEvent(long timeUntilNextCycle, List<Sellable> oldCycle) {
        this.timeUntilNextCycle = timeUntilNextCycle;

        Sellable sellableSoldMost = null;
        for (Sellable sellable : oldCycle) {
            Map.Entry<UUID, Integer> playerAmountPair = sellable.soldMostBy();
            if (playerAmountPair != null
                    && (sellableSoldMost == null
                    || sellable.soldMostBy().getValue() > sellableSoldMost.soldMostBy().getValue())) {
                sellableSoldMost = sellable;
            }
        }

        this.sellableSoldMost = sellableSoldMost;
    }

    public long getTimeUntilNextCycle() {
        return timeUntilNextCycle;
    }

    public Sellable getSellableSoldMost() {
        return sellableSoldMost;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @SuppressWarnings("unused")
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
