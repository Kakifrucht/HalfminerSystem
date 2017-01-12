package de.halfminer.hms.modules;

import de.halfminer.hms.cmd.abs.HalfminerPersistenceCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * - Stores and calls registered persistent commands to be executed when a given event is fired for a player
 */
@SuppressWarnings("unused")
public class ModCommandPersistence extends HalfminerModule implements Listener {

    private final Map<UUID, HalfminerPersistenceCommand> runOnInteract = new HashMap<>();

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (runOnInteract.containsKey(uuid)) {
            boolean keep = runOnInteract.get(uuid).execute(e);
            if (!keep) runOnInteract.remove(uuid);
        }
    }

    public void addPersistentCommand(HalfminerPersistenceCommand command) {
        switch (command.getMode()) {
            case EVENT_PLAYER_INTERACT:
                runOnInteract.put(command.getSenderUUID(), command);
                break;
        }
    }
}
