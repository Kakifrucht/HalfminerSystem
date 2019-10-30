package de.halfminer.hmh;

import de.halfminer.hmh.data.HaroConfig;
import de.halfminer.hmh.data.HaroPlayer;
import de.halfminer.hmh.data.HaroStorage;
import de.halfminer.hmh.tasks.TitleUpdateTask;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.handler.storage.PlayerNotFoundException;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.logging.Level;

/**
 * Listeners for Haro, handles login whitelist, checks if players are initialized, handles death kicking and respawning.
 */
public class HaroListeners extends HaroClass implements Listener {

    private final HanStorage systemStorage = hms.getStorageHandler();
    private final HaroStorage haroStorage = hmh.getHaroStorage();
    private final TitleUpdateTask titleUpdateTask = hmh.getTitleUpdateTask();


    @EventHandler
    public void onLogin(PlayerLoginEvent e) {

        if (e.getPlayer().hasPermission("hmh.admin")) {
            return;
        }

        HaroPlayer haroPlayer;
        try {
            HalfminerPlayer halfminerPlayer = systemStorage.getPlayer(e.getPlayer().getUniqueId());
            haroPlayer = haroStorage.getHaroPlayer(halfminerPlayer);
        } catch (PlayerNotFoundException ex) {
            // if player hasn't joined before, systemStorage.getPlayer will throw this exception in PlayerLoginEvent
            // migration from username to uuid in the db will be handled during login instead
            haroPlayer = haroStorage.getHaroPlayer(e.getPlayer().getName());
        }

        String kickMessageKey = null;
        if (!haroPlayer.isAdded()) {
            kickMessageKey = "listenerNotAdded";
        } else if (haroStorage.isGameRunning()) {

            if (haroPlayer.isEliminated()) {
                kickMessageKey = "listenerAlreadyEliminated";
            }

            if (!haroPlayer.hasTimeLeft()) {
                kickMessageKey = "listenerNoTimeLeft";
            }
        }

        if (kickMessageKey != null) {
            String kickMessage = MessageBuilder.returnMessage(kickMessageKey, hmh, false);
            e.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, kickMessage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        HalfminerPlayer hPlayer = hms.getStorageHandler().getPlayer(player);
        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(hPlayer);

        if (isInGame(player)) {
            hmh.getPlayerInitializer().initializePlayer(player);

            MessageBuilder.create("listenerJoinedInfo", hmh)
                    .addPlaceholder("TIMELEFT", haroPlayer.getTimeLeftSeconds() / 60)
                    .sendMessage(player);

            MessageBuilder.create("listenerJoinedLogTime", hmh)
                    .addPlaceholder("PLAYER", player.getName())
                    .addPlaceholder("TIMELEFT", haroPlayer.getTimeLeftSeconds())
                    .logMessage(Level.INFO);
        }

        titleUpdateTask.updateTitles();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        HaroPlayer haroPlayer = haroStorage.getHaroPlayer(player);
        haroPlayer.setOffline();

        if (haroStorage.isGameRunning() && isNotAdmin(player)) {

            HealthManager healthManager = hmh.getHealthManager();
            healthManager.storeCurrentHealth(player);
            healthManager.resetPlayerHealth(player);

            // hide quit message when kicking after dying, as we already broadcast a message in onDeath
            if (haroPlayer.isEliminated()) {
                e.setQuitMessage("");
            }
        }

        titleUpdateTask.updateTitles(true);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {

        Player player = e.getEntity();
        if (isInGame(player)) {

            HaroPlayer haroPlayer = haroStorage.getHaroPlayer(player);
            HaroConfig haroConfig = haroStorage.getHaroConfig();
            if (haroConfig.isHealthEnabled()) {

                Player killer = player.getKiller();

                HealthManager healthManager = hmh.getHealthManager();
                if (killer != null && !player.equals(killer)) {
                    setEliminatedAndKickPlayer(player, haroPlayer);
                    if (healthManager.increaseHealth(killer)) {
                        MessageBuilder.create("listenerHealthIncreased", hmh)
                                .addPlaceholder("NEWHEARTS", (int) healthManager.getMinecraftMaxHealth(killer) / 2)
                                .sendMessage(killer);
                    }
                } else {
                    if (healthManager.reduceHealth(player)) {
                        MessageBuilder.create("listenerHealthReduced", hmh)
                                .addPlaceholder("NEWHEARTS", (int) healthManager.getMinecraftMaxHealth(player) / 2)
                                .sendMessage(player);
                    }
                }

            } else {
                setEliminatedAndKickPlayer(player, haroPlayer);
            }
        }
    }

    private void setEliminatedAndKickPlayer(Player player, HaroPlayer haroPlayer) {
        haroPlayer.setEliminated(true);

        // kick on next tick, also moves the messages below the default death message in chat order
        scheduler.runTaskLater(hmh, () -> {
            String kickMessage = MessageBuilder.returnMessage("listenerDeathKick", hmh, false);
            player.kickPlayer(kickMessage);

            MessageBuilder.create("listenerDeathBroadcast", hmh)
                    .addPlaceholder("PLAYER", player.getName())
                    .addPlaceholder("COUNTALIVE", haroStorage.getAddedPlayers(true).size())
                    .broadcastMessage(false);

            if (haroStorage.isGameOver()) {
                HaroPlayer winner = haroStorage.getAddedPlayers(true).get(0);
                MessageBuilder.create("listenerGameWonBroadcast", hmh)
                        .addPlaceholder("PLAYER", winner.getName())
                        .broadcastMessage(true);

                if (winner.isOnline()) {
                    MessageBuilder.create("listenerGameWon", hmh).sendMessage(winner.getBase());
                }
            }
        }, 1L);
    }

    private boolean isInGame(Player player) {
        return haroStorage.isGameRunning()
                && !haroStorage.isGameOver()
                && isNotAdmin(player);
    }

    private boolean isNotAdmin(Player player) {
        return !player.hasPermission("hmh.admin");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Location spawn = haroStorage.getSpawnPoint();
        if (spawn != null) {
            e.setRespawnLocation(spawn);
        }
    }
}
