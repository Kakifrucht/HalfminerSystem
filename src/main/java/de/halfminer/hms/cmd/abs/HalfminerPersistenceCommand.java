package de.halfminer.hms.cmd.abs;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.modules.ModCommandPersistence;
import org.bukkit.command.CommandSender;
import org.bukkit.event.player.PlayerEvent;

import java.util.UUID;

/**
 * Called by {@link ModCommandPersistence} after being registered via {@link #setPersistent(PersistenceMode, UUID)}.
 */
@SuppressWarnings("unused")
public abstract class HalfminerPersistenceCommand extends HalfminerCommand {

    private final static ModCommandPersistence persistence =
            (ModCommandPersistence) hms.getModule(ModuleType.COMMAND_PERSISTENCE);

    private UUID persistenceSender;
    private UUID persistenceOwner;
    private PersistenceMode mode;

    /**
     * Execute with event passed by {@link ModCommandPersistence}.
     *
     * @param e Event that causes the execution of the command
     * @return true if it has executed successfully and should not be called anymore, false if it must be called again
     */
    public abstract boolean execute(PlayerEvent e);

    /**
     * Will be called if plugin is disabled while this command still persists.
     */
    public abstract void onDisable();

    /**
     * Get the persistence owners UUID.
     *
     * @return UUID of player that will trigger persistence module
     * @throws RuntimeException when persistence was not enabled
     */
    public UUID getPersistenceUUID() {
        if (persistenceOwner != null)
            return persistenceOwner;
        throw new RuntimeException("getPersistenceUUID() called before persistence was set");
    }

    public PersistenceMode getMode() {
        return mode;
    }

    /**
     * Enables persistence for this command, define on which event and for which UUID of a player.
     * After calling this method {@link #sender} and {@link #player} will be set to null to prevent
     * memory leaks. They can be referenced by {@link #getOriginalSender() getOriginalSender}
     * and {@link PlayerEvent#getPlayer() getPlayer}.
     *
     * @param mode which event will trigger this command
     * @param setTo sets the given UUID to the event trigger
     */
    protected void setPersistent(PersistenceMode mode, UUID setTo) {
        this.mode = mode;
        this.persistenceSender = isPlayer ? player.getUniqueId() : null;
        this.persistenceOwner = setTo;
        this.sender = null;
        this.player = null;
        persistence.addPersistentCommand(this);
    }

    protected CommandSender getOriginalSender() {
        if (isPlayer) {
            return player != null ? player : server.getPlayer(persistenceSender);
        } else {
            return sender != null ? sender : server.getConsoleSender();
        }
    }

    public enum PersistenceMode {
        EVENT_PLAYER_INTERACT,
        EVENT_PLAYER_JOIN
    }
}
