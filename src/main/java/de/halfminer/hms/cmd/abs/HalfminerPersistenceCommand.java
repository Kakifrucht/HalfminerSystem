package de.halfminer.hms.cmd.abs;

import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.modules.ModCommandPersistence;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Called by {@link ModCommandPersistence} after being registered via {@link #setPersistent(PersistenceMode)}.
 */
@SuppressWarnings("unused")
public abstract class HalfminerPersistenceCommand extends HalfminerCommand {

    private final static ModCommandPersistence persistence =
            (ModCommandPersistence) hms.getModule(ModuleType.COMMAND_PERSISTENCE);

    protected WeakReference<CommandSender> sender;
    protected WeakReference<Player> player;

    private UUID persistenceOwner;
    private PersistenceMode mode;

    protected void preExecute() {
        sender = new WeakReference<>(super.sender);
        super.sender = null;
        if (isPlayer) {
            player = new WeakReference<>(super.player);
            super.player = null;
        }
    }

    /**
     * Execute with event passed by {@link ModCommandPersistence}.
     *
     * @param e Event that causes the execution of the command
     * @return true if it has executed successfully and should not be called anymore, false if it must be called again
     */
    public abstract boolean execute(Event e);

    /**
     * Get the senders UUID.
     *
     * @return the senders UUID, or default "00000000-0000-0000-0000-000000000000"
     * UUID if not a player or not yet initialized
     */
    public UUID getSenderUUID() {
        if (persistenceOwner != null)
            return persistenceOwner;
        else {
            Player player1 = player.get();
            if (player1 != null)
                return player1.getUniqueId();
            else
                return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }

    protected void setPersistenceOwner(UUID uuidOfOwner) {
        persistenceOwner = uuidOfOwner;
    }

    //TODO add method that will be called if command is discarded due to shutdown

    public PersistenceMode getMode() {
        return mode;
    }

    protected void setPersistent(PersistenceMode mode) {
        this.mode = mode;
        persistence.addPersistentCommand(this);
    }

    public enum PersistenceMode {
        EVENT_PLAYER_INTERACT,
        EVENT_PLAYER_JOIN,
        NONE
    }
}
