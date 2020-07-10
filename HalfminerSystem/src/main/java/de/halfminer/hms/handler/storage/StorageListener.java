package de.halfminer.hms.handler.storage;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.handler.HanStorage;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Reloadable;
import de.halfminer.hms.util.Message;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link Listener} class that records default data about joining players, such as the online time, the
 * current and last seen name, the players {@link java.util.UUID} associated to the name and the timestamp
 * when the player was last seen on the server.
 */
public class StorageListener extends HalfminerClass implements Disableable, Listener, Reloadable {

    private final HanStorage storage;
    private Map<HalfminerPlayer, Long> timeOnline;


    public StorageListener(HanStorage storage) {
        this.storage = storage;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        HalfminerPlayer hPlayer = storage.getPlayer(player);

        storage.setUUID(player.getName(), player.getUniqueId());

        setLastSeen(player, Long.MAX_VALUE);
        timeOnline.put(hPlayer, System.currentTimeMillis() / 1000);

        // check and store previous name, broadcast name change
        final String previousName = hPlayer.getName();
        if (hPlayer.wasSeenBefore() && !previousName.equalsIgnoreCase(player.getName())) {

            boolean containsName = false;
            for (String name : hPlayer.getPreviousNames()) {
                if (name.equalsIgnoreCase(previousName)) {
                    containsName = true;
                    break;
                }
            }

            // don't add duplicates to database
            if (!containsName) {

                String previousNames = hPlayer.getString(DataType.PREVIOUS_NAMES);
                if (!previousNames.isEmpty()) {
                    previousNames += ' ';
                }
                previousNames += previousName;
                hPlayer.set(DataType.PREVIOUS_NAMES, previousNames);
            }

            Message.create("hanStorageNameChange", "Name")
                    .addPlaceholder("%OLDNAME%", previousName)
                    .addPlaceholder("%NAME%", player.getName())
                    .broadcast(true);
        }

        storage.getPlayer(player).set(DataType.LAST_NAME, player.getName());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();

        setOnlineTime(player, true);
        setLastSeen(player);
    }

    private void setOnlineTime(Player player, boolean remove) {

        HalfminerPlayer hPlayer = storage.getPlayer(player);
        if (timeOnline.containsKey(hPlayer)) {
            long lastTime = timeOnline.get(hPlayer);
            long currentTime = System.currentTimeMillis() / 1000;
            int time = (int) (currentTime - lastTime);

            storage.getPlayer(player).incrementInt(DataType.TIME_ONLINE, time);

            if (remove) {
                timeOnline.remove(hPlayer);
            } else {
                timeOnline.put(hPlayer, currentTime);
            }
        }
    }

    private void setLastSeen(Player player) {
        setLastSeen(player, System.currentTimeMillis() / 1000);
    }

    private void setLastSeen(Player player, long time) {
        storage.getPlayer(player).set(DataType.LAST_SEEN, time);
    }

    @Override
    public void loadConfig() {

        if (timeOnline == null) {
            timeOnline = new HashMap<>();
            long time = System.currentTimeMillis() / 1000;
            for (Player player : server.getOnlinePlayers()) {
                timeOnline.put(storage.getPlayer(player), time);
            }

            scheduler.runTaskTimer(hms, this::updateOnlineTimeAllPlayers, 1200L, 1200L);

            server.getOnlinePlayers().forEach(p -> setLastSeen(p, Long.MAX_VALUE));
        }
    }

    @Override
    public void onDisable() {
        updateOnlineTimeAllPlayers();
        server.getOnlinePlayers().forEach(this::setLastSeen);
    }

    private void updateOnlineTimeAllPlayers() {
        server.getOnlinePlayers().forEach(p -> setOnlineTime(p, false));
    }
}
