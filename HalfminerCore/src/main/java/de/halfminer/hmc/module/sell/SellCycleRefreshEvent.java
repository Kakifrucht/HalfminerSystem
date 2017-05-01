package de.halfminer.hmc.module.sell;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired by {@link SellableMap} whenever a new cycle starts.
 */
public class SellCycleRefreshEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    SellCycleRefreshEvent() {
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
