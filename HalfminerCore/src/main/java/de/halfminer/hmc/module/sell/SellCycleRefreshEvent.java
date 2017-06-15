package de.halfminer.hmc.module.sell;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * Fired by {@link SellableMap} whenever a new cycle starts.
 */
public class SellCycleRefreshEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final long timeUntilNextCycle;

    private final Sellable sellableMostSoldLastCycle;
    private final UUID uuidSoldMostLastCycle;
    private final int amountSoldMostLastCycle;

    SellCycleRefreshEvent(long timeUntilNextCycle,
                          Sellable sellableMostSoldLastCycle, UUID uuidSoldMostLastCycle, int amountSoldMostLastCycle) {
        this.timeUntilNextCycle = timeUntilNextCycle;
        this.sellableMostSoldLastCycle = sellableMostSoldLastCycle;
        this.uuidSoldMostLastCycle = uuidSoldMostLastCycle;
        this.amountSoldMostLastCycle = amountSoldMostLastCycle;
    }

    public long getTimeUntilNextCycle() {
        return timeUntilNextCycle;
    }

    public Sellable getSellableMostSoldLastCycle() {
        return sellableMostSoldLastCycle;
    }

    public UUID getUuidSoldMostLastCycle() {
        return uuidSoldMostLastCycle;
    }

    public int getAmountSoldMostLastCycle() {
        return amountSoldMostLastCycle;
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
