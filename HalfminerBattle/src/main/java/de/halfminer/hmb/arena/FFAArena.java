package de.halfminer.hmb.arena;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.FFAMode;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.util.CustomAction;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * TODO
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

        //TODO teleport delay, use HanTeleport?

        Long timestamp = bannedFromArena.getIfPresent(toAdd);
        if (timestamp != null) {
            long secondsLeft = (System.currentTimeMillis() / 1000) - timestamp;
            MessageBuilder.create(hmb, "modeFFACooldown", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%TIMELEFT%", String.valueOf(secondsLeft))
                    .sendMessage(toAdd);
            return false;
        }

        playersInArena.add(toAdd);
        storeClearAndTeleportPlayers(toAdd);
        healAndPreparePlayers(toAdd);
        equipPlayers(toAdd);
        return true;
    }

    public void removePlayer(Player toRemove) {
        restorePlayers(true, toRemove);
        playersInArena.remove(toRemove);
        streaks.remove(toRemove);
    }

    public void hasDied(Player hasDied) {

        int streakDied = streaks.containsKey(hasDied) ? Math.min(streaks.get(hasDied), 0) - 1 : -1;
        streaks.put(hasDied, streakDied);
        // respawn later
        //TODO also remove if logout before death
        hmb.getServer().getScheduler().runTaskLater(hmb, () -> {
            if (hasDied.isOnline()) {
                hasDied.spigot().respawn();

                if (-streakDied == gameMode.getRemoveAfterDeaths()) {
                    MessageBuilder.create(hmb, "modeFFADiedTooOften", HalfminerBattle.PREFIX).sendMessage(hasDied);
                    removePlayer(hasDied);
                    bannedFromArena.put(hasDied.getUniqueId(), System.currentTimeMillis() / 1000);
                } else teleportIntoArena(hasDied);
            }
        }, 2L);

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
