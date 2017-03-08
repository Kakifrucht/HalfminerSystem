package de.halfminer.hmc.cmd.abs;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;

import java.util.UUID;

/**
 * Commands that have their own listeners
 */
@SuppressWarnings("unused")
public abstract class HalfminerPersistenceCommand extends HalfminerCommand implements Listener {

    private UUID persistenceSender;
    private UUID persistenceOwner;

    protected boolean isPersistenceOwner(Player toCheck) {
        return toCheck.getUniqueId().equals(persistenceOwner);
    }

    /**
     * Enables persistence for this command, define on which event and for which UUID of a player.
     * After calling this method {@link #sender} and {@link #player} will be set to null to prevent
     * memory leaks. They can be referenced by {@link #getOriginalSender() getOriginalSender}
     * and {@link PlayerEvent#getPlayer() getPlayer}.
     *
     * @param setTo sets the given UUID to the event trigger
     */
    protected void setPersistent(UUID setTo) {
        this.persistenceSender = isPlayer ? player.getUniqueId() : null;
        this.persistenceOwner = setTo;
        this.sender = null;
        this.player = null;
        registerClass();
    }

    protected CommandSender getOriginalSender() {
        if (isPlayer) {
            return player != null ? player : server.getPlayer(persistenceSender);
        } else {
            return sender != null ? sender : server.getConsoleSender();
        }
    }
}
