package de.halfminer.hmb.arena;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.util.CustomAction;
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

    private final Map<Player, Integer> streaks = new HashMap<>();
    private final Cache<UUID, Boolean> bannedFromArena = CacheBuilder.newBuilder()
            .expireAfterWrite(4L, TimeUnit.MINUTES) //TODO parameters
            .build();

    public FFAArena(GameModeType gameMode, String name) {
        super(gameMode, name);
    }

    public void addPlayer(Player toAdd) {
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
        // respawn later
        hmb.getServer().getScheduler().runTaskLater(hmb, () -> {
            if (hasDied.isOnline()) {
                hasDied.spigot().respawn();

                if (streakDied == 4) {
                    //TODO params and messages
                    removePlayer(hasDied);
                    bannedFromArena.put(hasDied.getUniqueId(), true);
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
                    //TODO
                    e.printStackTrace();
                }
            }
        }
    }
}
