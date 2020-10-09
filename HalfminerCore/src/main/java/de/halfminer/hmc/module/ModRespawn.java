package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.cache.CustomAction;
import de.halfminer.hms.cache.exceptions.CachingException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * - Respawns player at custom location
 *   - Spawns player at custom location when travelling through end portal, and a point has been set with /setspawn end
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
    private Location endPortalLocation;
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

            // send different message if player already has online time in his stats (if played longer than 15 minutes)
            boolean hasActuallyPlayedBefore = storage.getPlayer(joined).getInt(DataType.TIME_ONLINE) > 15 * 60;
            message = Message.create(hasActuallyPlayedBefore ? "modRespawnFirstJoinHasPlayed" : "modRespawnFirstJoin", hmc)
                    .addPlaceholder("%PLAYER%", joined.getName())
                    .returnMessage();

            scheduler.runTaskLater(hmc, () -> {

                newPlayers.put(joined.getName().toLowerCase(), joined.getUniqueId());
                joined.teleport(respawnLoc);
                if (firstSpawnCommand.length() > 0) {

                    server.dispatchCommand(server.getConsoleSender(),
                            Message.create(firstSpawnCommand, hmc)
                                    .setDirectString()
                                    .addPlaceholder("%PLAYER%", joined.getName())
                                    .returnMessage());
                }
            }, 1L);

        } else if (toTeleportOnJoin.contains(joined)) {
            scheduler.runTaskLater(hmc, () -> {
                joined.teleport(respawnLoc);
                Message.create("modRespawnForced", hmc, "Spawn").send(joined);
                hmc.getLogger().info("Respawn: Force-teleported " + joined.getName() + " to spawn");
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
            titleHandler.sendTitle(p, Message.returnMessage("modRespawnWelcomeBonusTitle", hmc), 10, 30, 0);
            barHandler.sendBar(p, Message.returnMessage("modRespawnWelcomeBonusBossbar", hmc),
                    BarColor.WHITE, BarStyle.SOLID, 10, 1.0d);

            scheduler.runTaskLater(hmc, () -> {

                if (Utils.random(randomRange)) {

                    action.addPlaceholderForNextRun("%CUSTOM%",
                            Message.returnMessage("modRespawnWelcomeBonusCustom", hmc));

                    if (action.runAction(p)) {
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                        titleHandler.sendTitle(p,
                                Message.returnMessage("modRespawnWelcomeBonusTitleYes", hmc), 0, 60, 10);
                        return;
                    }
                }

                titleHandler.sendTitle(p, Message.returnMessage("modRespawnWelcomeBonusTitleNo", hmc), 0, 30, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_ARROW_HIT, 1.0f, 2.0f);
            }, 20L);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalEvent(PlayerPortalEvent e) {

        if (e.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL && endPortalLocation != null) {
            scheduler.runTask(hmc, () -> {
                if (!e.isCancelled()) {
                    e.getPlayer().teleport(endPortalLocation);
                }
            });
            // simpler solution that doesn't work: e.setTo(endPortalLocation);
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
        coreStorage.set("respawn.spawnLocation", loc);
        loc.getWorld().setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean setEndPortalSpawn(Location loc) {

        if (loc == null || !loc.getWorld().getEnvironment().equals(World.Environment.THE_END)) {
            return false;
        }

        endPortalLocation = loc;
        coreStorage.set("respawn.spawnEndLocation", loc);
        return true;
    }

    @Override
    public void sweep() {
        newPlayers.cleanUp();
        lastWelcome.cleanUp();
    }

    @Override
    public void loadConfig() {

        ConfigurationSection config = hmc.getConfig().getConfigurationSection("respawn");

        Object spawnLocation = coreStorage.get("respawn.spawnLocation");
        if (spawnLocation instanceof Location) {
            respawnLoc = (Location) spawnLocation;
        } else {
            respawnLoc = server.getWorlds().get(0).getSpawnLocation();
        }

        Object spawnEndLocation = coreStorage.get("respawn.spawnEndLocation");
        if (spawnEndLocation instanceof Location) {
            endPortalLocation = (Location) spawnEndLocation;
        } else {
            endPortalLocation = null;
        }

        firstSpawnCommand = config.getString("firstJoinCommand", "");

        welcomeWords = new HashSet<>();
        welcomeWords.addAll(config.getStringList("welcomeBonus.words")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList()));

        try {
            action = new CustomAction(config.getString("welcomeBonus.customAction", "nothing"), coreStorage);
        } catch (CachingException e) {
            Message.create("modRespawnWelcomeBonusActionError", hmc)
                    .addPlaceholder("%REASON%", e.getCleanReason())
                    .log(Level.WARNING);
            try {
                action = new CustomAction("nothing", coreStorage);
            } catch (CachingException ignored) {
                // cannot happen, as CachingException's are not thrown on init of empty CustomAction
            }
        }

        int timeForWelcomeSeconds = config.getInt("welcomeBonus.timeSeconds", 300);
        randomRange = config.getInt("welcomeBonus.randomRange", 1);

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
