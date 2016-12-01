package de.halfminer.hms.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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
import java.util.concurrent.TimeUnit;
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
 *   - Uses (sub)title and bossbar for information and plays sounds
 */
public class ModRespawn extends HalfminerModule implements Listener, Sweepable {

    private Location respawnLoc;
    private String firstSpawnCommand;

    private final Set<OfflinePlayer> toTeleportOnJoin = new HashSet<>();

    private Cache<String, UUID> newPlayers;
    private Cache<UUID, Boolean> lastWelcome;

    // config random drop
    private Set<String> welcomeWords;
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

            message = MessageBuilder.create(hms, "modRespawnFirstJoin")
                    .addPlaceholderReplace("%PLAYER%", joined.getName())
                    .returnMessage();

            scheduler.runTaskLater(hms, () -> {

                newPlayers.put(joined.getName().toLowerCase(), joined.getUniqueId());
                joined.teleport(respawnLoc);
                if (firstSpawnCommand.length() > 0) {

                    server.dispatchCommand(server.getConsoleSender(),
                            MessageBuilder.create(hms, firstSpawnCommand)
                                    .setMode(MessageBuilder.Mode.DIRECT_STRING)
                                    .addPlaceholderReplace("%PLAYER%", joined.getName())
                                    .returnMessage());
                }
            }, 1L);

        } else if (toTeleportOnJoin.contains(joined)) {
            scheduler.runTaskLater(hms, () -> {
                joined.teleport(respawnLoc);
                MessageBuilder.create(hms, "modRespawnForced", "Spawn").sendMessage(joined);
                toTeleportOnJoin.remove(joined);
            }, 1L);
        }

        e.setJoinMessage(message);
    }

    // priority is at high, because Factions cancels chat events to send out individual messages
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChatWelcomeReward(AsyncPlayerChatEvent e) {

        Player p = e.getPlayer();
        if (welcomeWords.size() == 0 || lastWelcome.getIfPresent(p.getUniqueId()) != null) return;

        Player mentioned = null;
        boolean containsWelcome = false;

        for (String message : e.getMessage().toLowerCase().split(" ")) {

            if (containsWelcome && mentioned != null) break;

            if (!containsWelcome && welcomeWords.contains(message)) containsWelcome = true;
            else if (mentioned == null) {

                String normalized = Utils.filterNonUsernameChars(message);
                if (!normalized.equalsIgnoreCase(p.getName())) {
                    UUID uuid = newPlayers.getIfPresent(normalized);
                    if (uuid != null) mentioned = server.getPlayer(uuid);
                }
            }
        }

        if (containsWelcome && mentioned != null) {

            lastWelcome.put(p.getUniqueId(), true);
            titleHandler.sendTitle(p, MessageBuilder.returnMessage(hms, "modRespawnHeadTitle"), 10, 30, 0);
            barHandler.sendBar(p, MessageBuilder.returnMessage(hms, "modRespawnHeadBossbar"),
                    BarColor.WHITE, BarStyle.SOLID, 10, 1.0d);

            scheduler.runTaskLater(hms, () -> {

                if (new Random().nextInt(1000) < randomRange && Utils.hasRoom(p, 1)) {

                    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1);
                    skull.setDurability((short) 3);
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    meta.setOwner(p.getName());
                    meta.setLore(Collections.singletonList(MessageBuilder.returnMessage(hms, "modRespawnHeadLore")));
                    skull.setItemMeta(meta);

                    p.getInventory().addItem(skull);
                    MessageBuilder.create(hms, "modRespawnHeadBroadcast", "Skull")
                            .addPlaceholderReplace("%PLAYER%", p.getName())
                            .broadcastMessage(true);
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                    titleHandler.sendTitle(p, MessageBuilder.returnMessage(hms, "modRespawnHeadTitleYes"), 0, 60, 10);
                } else {
                    titleHandler.sendTitle(p, MessageBuilder.returnMessage(hms, "modRespawnHeadTitleNo"), 0, 30, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 2.0f);
                }
            }, 20L);
        }
    }

    public Location getSpawn() {
        return respawnLoc;
    }

    void tpToSpawn(Player p) {
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

    @Override
    public void sweep() {
        newPlayers.cleanUp();
        lastWelcome.cleanUp();
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

        int timeForWelcomeSeconds = hms.getConfig().getInt("respawn.timeForWelcomeSeconds", 300);

        newPlayers = Utils.copyValues(newPlayers,
                CacheBuilder.newBuilder()
                .expireAfterWrite(timeForWelcomeSeconds, TimeUnit.SECONDS)
                .build());

        lastWelcome = Utils.copyValues(lastWelcome,
                CacheBuilder.newBuilder()
                        .expireAfterWrite(timeForWelcomeSeconds, TimeUnit.SECONDS)
                        .build());

        randomRange = hms.getConfig().getInt("respawn.randomRange", 1);
    }
}
