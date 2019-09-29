package de.halfminer.hmh.cmd;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Location;

/**
 * - Will set the spawn point to the location the executing player is currently at.
 * - Spawn point will be used for respawns, and as the starting point when the game starts.
 * - Access the spawn point by using /harospawn (or /spawn, if not overloaded).
 */
public class Cmdsetspawn extends HaroCommand {

    private final boolean teleport;

    public Cmdsetspawn(boolean teleport) {
        super("setspawn");
        this.teleport = teleport;
    }

    @Override
    protected void execute() {

        if (!isPlayer) {
            sendNotAPlayerMessage();
            return;
        }

        if (teleport) {
            Location spawnPoint = haroStorage.getSpawnPoint();
            if (spawnPoint != null) {
                hms.getTeleportHandler().startTeleport(player, spawnPoint);
            } else {
                MessageBuilder.create("cmdSetspawnTeleportNotSet", hmh).sendMessage(sender);
            }

        } else /* set teleport */ {
            haroStorage.setSpawnPoint(player.getLocation());
            MessageBuilder.create("cmdSetspawnSet", hmh).sendMessage(sender);
        }
    }
}
