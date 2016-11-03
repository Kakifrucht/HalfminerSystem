package de.halfminer.hms.modules;

import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.Language;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * - Respawns player at custom location
 * - Adds a first time join
 *   - Else, removes join message
 *   - Execute custom command on first join
 * - Adds random chance to get own head dropped if new players are being welcomed
 *   - Custom welcome words
 *   - Cooldown to prevent misuse
 *   - Custom probability
 */
public class ModRespawn extends HalfminerModule implements Listener, Sweepable {

    private Location respawnLoc;
    private String firstSpawnCommand;

    private final Set<OfflinePlayer> toTeleportOnJoin = new HashSet<>();

    private final Map<String, UUID> newPlayers = new HashMap<>();
    private final Map<UUID, Long> lastWelcome = new ConcurrentHashMap<>();

    // config random drop
    private Set<String> welcomeWords;
    private int timeForWelcomeSeconds;
    private int randomRange;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        e.setRespawnLocation(respawnLoc);
    }

    @EventHandler
    public void onFirstJoin(final PlayerJoinEvent e) {

        String message = "";
        final Player joined = e.getPlayer();

        if (!joined.hasPlayedBefore()) {

            message = Language.getMessagePlaceholders("modRespawnFirstJoin", false, "%PLAYER%", joined.getName());
            newPlayers.put(joined.getName().toLowerCase(), joined.getUniqueId());

            scheduler.runTaskLater(hms, () -> {

                joined.teleport(respawnLoc);
                if (firstSpawnCommand.length() > 0) {
                    server.dispatchCommand(server.getConsoleSender(),
                            Language.placeholderReplace(firstSpawnCommand, "%PLAYER%", joined.getName()));
                }
            }, 1L);

            scheduler.runTaskLater(hms, () ->
                    newPlayers.remove(joined.getName().toLowerCase()), timeForWelcomeSeconds * 20L);

        } else if (toTeleportOnJoin.contains(joined)) {
            scheduler.runTaskLater(hms, () -> {
                joined.teleport(respawnLoc);
                joined.sendMessage(Language.getMessagePlaceholders("modRespawnForced", true, "%PREFIX%", "Spawn"));
                toTeleportOnJoin.remove(joined);
            }, 1L);
        }

        e.setJoinMessage(message);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChatWelcomeReward(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (welcomeWords.size() == 0 || !canWelcomePlayer(p.getUniqueId())) return;

        Player mentioned = null;
        boolean containsWelcome = false;

        for (String message : e.getMessage().split(" ")) {
            if (welcomeWords.contains(message)) containsWelcome = true;
            else {

                String normalized = Language.filterNonUsernameChars(message).toLowerCase();
                if (newPlayers.containsKey(normalized) && !normalized.equalsIgnoreCase(p.getName())) {
                    OfflinePlayer player = server.getOfflinePlayer(newPlayers.get(normalized));
                    if (player.isOnline()) {
                        mentioned = (Player) player;
                        if (containsWelcome) break;
                    }
                }
            }

        }

        if (containsWelcome && mentioned != null) {

            lastWelcome.put(p.getUniqueId(), System.currentTimeMillis());
            titleHandler.sendActionBar(p, "Head drop...");
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException ignored) {}

            if (new Random().nextInt(1000) < randomRange) {
                titleHandler.sendActionBar(p, "&a&lYES");
                scheduler.runTask(hms, () -> {

                    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1);
                    skull.setDurability((short) 3);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    meta.setOwner(p.getName());
                    meta.setLore(Collections.singletonList(Language.getMessage("modRespawnHeadLore")));
                    skull.setItemMeta(meta);

                    if (p.getInventory().addItem(skull).size() == 0) {
                        server.broadcast(Language.getMessagePlaceholders("modRespawnHeadBroadcast", true,
                                "%PREFIX%", "Skull", "%PLAYER%", p.getName()), "hms.default");
                    }
                });
            } else titleHandler.sendActionBar(p, "&cNo");
        }
    }

    public Location getSpawn() {
        return respawnLoc;
    }

    public void tpToSpawn(Player p) {
        p.teleport(respawnLoc);
    }

    public boolean teleportToSpawnOnJoin(OfflinePlayer p) {

        if (toTeleportOnJoin.contains(p)) {
            toTeleportOnJoin.remove(p);
            return false;
        } else {
            toTeleportOnJoin.add(p);
            return true;
        }
    }

    public void setSpawn(Location loc) {

        respawnLoc = loc;
        storage.set("spawnlocation", loc);
        loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private boolean canWelcomePlayer(UUID uuid) {

        return !lastWelcome.containsKey(uuid)
                || lastWelcome.get(uuid) + (timeForWelcomeSeconds * 1000) < System.currentTimeMillis();
    }

    @Override
    public void sweep() {

        Iterator<UUID> it = lastWelcome.keySet().iterator();
        while (it.hasNext()) {
            if (canWelcomePlayer(it.next())) it.remove();
        }
    }

    @Override
    public void loadConfig() {

        Object loc = storage.get("spawnlocation");
        if (loc instanceof Location) respawnLoc = (Location) loc;
        else respawnLoc = server.getWorlds().get(0).getSpawnLocation();

        firstSpawnCommand = hms.getConfig().getString("respawn.firstJoinCommand", "");

        welcomeWords = new HashSet<>();
        welcomeWords.addAll(hms.getConfig().getStringList("respawn.welcomeWords")
                .stream().map(String::toLowerCase).collect(Collectors.toList()));

        timeForWelcomeSeconds = hms.getConfig().getInt("respawn.timeForWelcomeSeconds", 300);
        randomRange = hms.getConfig().getInt("respawn.randomRange", 1);
    }
}
