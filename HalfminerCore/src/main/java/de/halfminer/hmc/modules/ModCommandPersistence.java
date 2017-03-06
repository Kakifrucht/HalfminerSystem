package de.halfminer.hmc.modules;

import de.halfminer.hmc.cmd.abs.HalfminerPersistenceCommand;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.util.Pair;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * - Stores and calls registered persistent commands to be executed when a given event is fired for a player
 */
@SuppressWarnings("unused")
public class ModCommandPersistence extends HalfminerModule implements Disableable, Listener {

    private final Map<Pair<UUID, HalfminerPersistenceCommand.PersistenceMode>,
            HalfminerPersistenceCommand> runOnEvent = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        execute(e, HalfminerPersistenceCommand.PersistenceMode.EVENT_PLAYER_INTERACT);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        execute(e, HalfminerPersistenceCommand.PersistenceMode.EVENT_PLAYER_JOIN);
    }

    private void execute(PlayerEvent e, HalfminerPersistenceCommand.PersistenceMode mode) {
        Pair<UUID, HalfminerPersistenceCommand.PersistenceMode> check = new Pair<>(e.getPlayer().getUniqueId(), mode);
        if (runOnEvent.containsKey(check)) {
            boolean isDone = runOnEvent.get(check).execute(e);
            if (isDone) runOnEvent.remove(check);
        }
    }

    public void addPersistentCommand(HalfminerPersistenceCommand command) {
        runOnEvent.put(new Pair<>(command.getPersistenceUUID(), command.getMode()), command);
    }

    @Override
    public void onDisable() {
        runOnEvent.values().forEach(HalfminerPersistenceCommand::onDisable);
    }
}
