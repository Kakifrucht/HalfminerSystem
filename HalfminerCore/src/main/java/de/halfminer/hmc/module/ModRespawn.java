package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.exceptions.CachingException;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.cache.CustomAction;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
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

    // config random action welcome bonus
    private Set<String> welcomeWords;
    private CustomAction action;
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

            message = MessageBuilder.create("modRespawnFirstJoin", hmc)
                    .addPlaceholderReplace("%PLAYER%", joined.getName())
                    .returnMessage();

            scheduler.runTaskLater(hmc, () -> {

                newPlayers.put(joined.getName().toLowerCase(), joined.getUniqueId());
                joined.teleport(respawnLoc);
                if (firstSpawnCommand.length() > 0) {

                    server.dispatchCommand(server.getConsoleSender(),
                            MessageBuilder.create(firstSpawnCommand, hmc)
                                    .setDirectString()
                                    .addPlaceholderReplace("%PLAYER%", joined.getName())
                                    .returnMessage());
                }
            }, 1L);

        } else if (toTeleportOnJoin.contains(joined)) {
            scheduler.runTaskLater(hmc, () -> {
                joined.teleport(respawnLoc);
                MessageBuilder.create("modRespawnForced", hmc, "Spawn").sendMessage(joined);
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
            titleHandler.sendTitle(p, MessageBuilder.returnMessage("modRespawnWelcomeBonusTitle", hmc), 10, 30, 0);
            barHandler.sendBar(p, MessageBuilder.returnMessage("modRespawnWelcomeBonusBossbar", hmc),
                    BarColor.WHITE, BarStyle.SOLID, 10, 1.0d);

            scheduler.runTaskLater(hmc, () -> {

                if (Utils.random(randomRange)) {

                    action.addPlaceholderForNextRun("%CUSTOM%",
                            MessageBuilder.returnMessage("modRespawnWelcomeBonusCustom", hmc));

                    if (action.runAction(p)) {
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                        titleHandler.sendTitle(p,
                                MessageBuilder.returnMessage("modRespawnWelcomeBonusTitleYes", hmc), 0, 60, 10);
                        return;
                    }
                }

                titleHandler.sendTitle(p, MessageBuilder.returnMessage("modRespawnWelcomeBonusTitleNo", hmc), 0, 30, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 2.0f);
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
        coreStorage.set("spawnlocation", loc);
        loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public void sweep() {
        newPlayers.cleanUp();
        lastWelcome.cleanUp();
    }

    @Override
    public void loadConfig() {

        FileConfiguration config = hmc.getConfig();

        Object loc = coreStorage.get("spawnlocation");
        if (loc instanceof Location) respawnLoc = (Location) loc;
        else respawnLoc = server.getWorlds().get(0).getSpawnLocation();

        firstSpawnCommand = config.getString("respawn.firstJoinCommand", "");

        welcomeWords = new HashSet<>();
        welcomeWords.addAll(config.getStringList("respawn.welcomeWords")
                .stream().map(String::toLowerCase).collect(Collectors.toList()));

        try {
            action = new CustomAction(config.getString("respawn.customActionWelcomeBonus", "nothing"), coreStorage);
        } catch (CachingException e) {
            MessageBuilder.create("modRespawnWelcomeBonusActionError", hmc)
                    .addPlaceholderReplace("%REASON%", e.getCleanReason())
                    .logMessage(Level.WARNING);
            try {
                action = new CustomAction("nothing", coreStorage);
            } catch (CachingException ignored) {
                // cannot happen, as CachingException's are not thrown on init of empty CustomAction
            }
        }

        int timeForWelcomeSeconds = config.getInt("respawn.timeForWelcomeSeconds", 300);
        randomRange = hmc.getConfig().getInt("respawn.randomRange", 1);

        newPlayers = Utils.copyValues(newPlayers,
                CacheBuilder.newBuilder()
                .expireAfterWrite(timeForWelcomeSeconds, TimeUnit.SECONDS)
                .build());

        lastWelcome = Utils.copyValues(lastWelcome,
                CacheBuilder.newBuilder()
                        .expireAfterWrite(timeForWelcomeSeconds, TimeUnit.SECONDS)
                        .build());
    }
}