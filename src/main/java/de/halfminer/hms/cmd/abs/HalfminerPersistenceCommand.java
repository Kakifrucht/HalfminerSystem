package de.halfminer.hms.cmd.abs;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.modules.ModCommandPersistence;
import org.bukkit.event.Event;

import java.util.UUID;

/**
 * Called by {@link ModCommandPersistence} after being registered via {@link #setPersistent(PersistenceMode)}.
 */
@SuppressWarnings("unused")
public abstract class HalfminerPersistenceCommand extends HalfminerCommand {

    private final static ModCommandPersistence persistence =
            (ModCommandPersistence) hms.getModule(ModuleType.COMMAND_PERSISTENCE);

    private PersistenceMode mode;

    /**
     * Execute with event passed by {@link ModCommandPersistence}.
     *
     * @param e Event that causes the execution of the command
     * @return true if it should be called by the next event again, false if it should be removed
     */
    public abstract boolean execute(Event e);

    /**
     * Get the senders UUID.
     *
     * @return the senders UUID, or default "00000000-0000-0000-0000-000000000000"
     * UUID if not a player or not yet initialized
     */
    public UUID getSenderUUID() {
        if (player != null) return player.getUniqueId();
        else return UUID.fromString("00000000-0000-0000-0000-000000000000");
    }

    public PersistenceMode getMode() {
        return mode;
    }

    public enum PersistenceMode {
        EVENT_PLAYER_INTERACT
    }

    protected void setPersistent(PersistenceMode mode) {
        this.mode = mode;
        persistence.addPersistentCommand(this);
    }
}
