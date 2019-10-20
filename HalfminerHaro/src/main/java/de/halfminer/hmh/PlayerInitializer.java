package de.halfminer.hmh;

import de.halfminer.hmh.data.HaroConfig;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.HaroStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Initializes players if the game is running after joining.
 * Handles setting of kick time, starting health, player init commands and spawn point teleport.
 * Only runs init commands / settings once per player, per game.
 */
public class PlayerInitializer extends HaroClass {

    private final HaroStorage haroStorage;


    PlayerInitializer() {
        super(false);
        this.haroStorage = hmh.getHaroStorage();

        // set players online, if plugin was completely reloaded and players are already online
        if (haroStorage.isGameRunning()) {
            server.getOnlinePlayers()
                    .stream()
                    .filter(p -> !p.hasPermission("hmh.admin"))
                    .forEach(this::initializePlayer);
        }
    }

    public void initializePlayer(Player player) {

        HaroConfig haroConfig = haroStorage.getHaroConfig();
        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(player);
        haroPlayer.setTimeUntilKick(System.currentTimeMillis() / 1000L + haroPlayer.getTimeLeftSeconds());

        if (!haroPlayer.isInitialized()) {

            double maxHealthAtStart = haroConfig.getStartingMaxHealth();
            haroPlayer.setHealth(maxHealthAtStart, maxHealthAtStart);

            List<String> playerInitCommands = haroConfig.getPlayerInitCommands();
            for (String playerInitCommand : playerInitCommands) {
                String command = playerInitCommand.replace("%PLAYER%", haroPlayer.getName());
                server.dispatchCommand(server.getConsoleSender(), command);
            }

            // check distance to spawn, teleport if too far away
            Location spawnPoint = haroStorage.getSpawnPoint();
            if (spawnPoint != null) {
                double maxDistance = haroConfig.getMaxSpawnDistance();
                if (spawnPoint.distance(player.getLocation()) > maxDistance) {
                    player.teleport(spawnPoint);
                }
            }

            haroPlayer.setInitialized(true);
            hmh.getLogger().info("Player " + player.getName() + " was initialized");
        }

        hmh.getHealthManager().updateHealth(player);
    }
}
