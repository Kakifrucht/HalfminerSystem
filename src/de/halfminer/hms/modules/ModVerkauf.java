package de.halfminer.hms.modules;

import de.halfminer.hms.interfaces.Sweepable;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/**
 * - Auto sells chests on open
 * - Allows easy toggling
 */
public class ModVerkauf extends HalfminerModule implements Listener, Sweepable {

    private final Set<UUID> autoSelling = new HashSet<>();

    @EventHandler
    public void onChestOpen(InventoryOpenEvent e) {

        if (e.getInventory().getHolder() instanceof Chest || e.getInventory().getHolder() instanceof DoubleChest) {

        }
    }

    public boolean togglePlayer(Player player) {

        UUID uuid = player.getUniqueId();
        boolean contains = autoSelling.contains(uuid);

        if (contains) autoSelling.remove(uuid);
        else autoSelling.add(uuid);

        return !contains;
    }

    @Override
    public void loadConfig() {

    }

    @Override
    public void sweep() {

        Iterator<UUID> it = autoSelling.iterator();
        while (it.hasNext()) {
            OfflinePlayer player = server.getOfflinePlayer(it.next());
            if (player.getLastPlayed() + 10000 < System.currentTimeMillis()) it.remove();
        }
    }
}
