package de.halfminer.hmb.arena;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.FFAMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.handlers.HanTeleport;
import de.halfminer.hms.util.CustomAction;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Free for all arena used by {@link FFAMode}, implementing custom killstreaks, timeouts and auto respawns
 */
@SuppressWarnings("unused")
public class FFAArena extends AbstractKitArena {

    private FFAMode gameMode = (FFAMode) getGameMode();

    private final Map<Player, Integer> streaks = new HashMap<>();
    private final Cache<UUID, Long> bannedFromArena = CacheBuilder.newBuilder()
            .expireAfterWrite(gameMode.getRemoveForMinutes(), TimeUnit.MINUTES)
            .build();

    public FFAArena(String name) {
        super(GameModeType.FFA, name);
    }

    public boolean addPlayer(Player toAdd) {

        Long timestamp = bannedFromArena.getIfPresent(toAdd);
        if (timestamp != null) {
            long secondsLeft = (System.currentTimeMillis() / 1000) - timestamp;
            MessageBuilder.create(hmb, "modeFFACooldown", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%TIMELEFT%", String.valueOf(secondsLeft))
                    .sendMessage(toAdd);
            return false;
        }

        Location spawn = spawns.get(0);
        HanTeleport teleportHandler = (HanTeleport) HalfminerSystem.getInstance().getHandler(HandlerType.TELEPORT);
        teleportHandler.startTeleport(toAdd, spawn, 3, null, () -> addPlayerInternal(toAdd));

        return true;
    }

    private void addPlayerInternal(Player toAdd) {
        playersInArena.add(toAdd);
        storeClearAndTeleportPlayers(toAdd);
        healAndPreparePlayers(toAdd);
        equipPlayers(toAdd);
    }

    public void removePlayer(Player toRemove) {
        restorePlayers(true, toRemove);
        playersInArena.remove(toRemove);
        streaks.remove(toRemove);
    }

    public void hasDied(Player hasDied) {

        int streakDied = streaks.containsKey(hasDied) ? Math.min(streaks.get(hasDied), 0) - 1 : -1;
        streaks.put(hasDied, streakDied);

        if (-streakDied == gameMode.getRemoveAfterDeaths()) {
            MessageBuilder.create(hmb, "modeFFADiedTooOften", HalfminerBattle.PREFIX).sendMessage(hasDied);
            removePlayer(hasDied);
            bannedFromArena.put(hasDied.getUniqueId(), System.currentTimeMillis() / 1000);
        } else {
            // respawn later
            hmb.getServer().getScheduler().runTaskLater(hmb, () -> {
                if (hasDied.isOnline() && pm.getArena(hasDied).equals(this)) {
                    hasDied.spigot().respawn();
                    teleportIntoArena(hasDied);
                }
            }, 2L);
        }

        Player killer = hasDied.getKiller();
        if (killer != null) {
            if (!playersInArena.contains(killer)) {
                hmb.getLogger().severe("Killer of " + hasDied.getName() + " " + killer.getName() + " not in Arena");
                return;
            }

            int streak = streaks.get(killer) + 1;
            streaks.put(killer, streak);

            try {
                CustomAction action = new CustomAction("ffa-" + streak, hmb.getCacheHolder());
                action.runAction(killer, hasDied);
            } catch (CachingException e) {
                if (!e.getReason().equals(CachingException.Reason.CHAPTER_NOT_FOUND)
                    && !e.getReason().equals(CachingException.Reason.FILE_EMPTY)) {
                    hmb.getLogger().warning("Error reading FFA killstreaks, Reason: " + e.getCleanReason());
                    e.printStackTrace();
                }
            }
        }
    }
}
