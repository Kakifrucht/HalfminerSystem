package de.halfminer.hms.modules;

import de.halfminer.hms.cmd.abs.HalfminerPersistenceCommand;
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
public class ModCommandPersistence extends HalfminerModule implements Listener {

    //TODO generify properly
    private final Map<UUID, HalfminerPersistenceCommand> runOnInteract = new HashMap<>();
    private final Map<UUID, HalfminerPersistenceCommand> runOnJoin = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        execute(e, runOnInteract);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        execute(e, runOnJoin);
    }

    private void execute(PlayerEvent e, Map<UUID, HalfminerPersistenceCommand> storage) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (storage.containsKey(uuid)) {
            boolean isDone = storage.get(uuid).execute(e);
            if (isDone) storage.remove(uuid);
        }
    }

    public void addPersistentCommand(HalfminerPersistenceCommand command) {
        switch (command.getMode()) {
            case EVENT_PLAYER_INTERACT:
                runOnInteract.put(command.getSenderUUID(), command);
                break;
            case EVENT_PLAYER_JOIN:
                runOnJoin.put(command.getSenderUUID(), command);
                break;
            case NONE:
                runOnInteract.entrySet().removeIf(entry -> entry.getValue().equals(command));
                runOnJoin.entrySet().removeIf(entry -> entry.getValue().equals(command));
                break;
        }
    }
}
