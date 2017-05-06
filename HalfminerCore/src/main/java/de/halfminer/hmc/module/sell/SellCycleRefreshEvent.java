package de.halfminer.hmc.module.sell;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired by {@link SellableMap} whenever a new cycle starts.
 */
public class SellCycleRefreshEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final long timeUntilNextCycle;

    SellCycleRefreshEvent(long timeUntilNextCycle) {
        this.timeUntilNextCycle = timeUntilNextCycle;
    }

    public long getTimeUntilNextCycle() {
        return timeUntilNextCycle;
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
