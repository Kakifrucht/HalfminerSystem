package de.halfminer.hmb.arena;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.mode.FFAMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.handlers.HanTeleport;
import de.halfminer.hms.util.CustomAction;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Free for all arena used by {@link FFAMode}, implementing custom killstreaks, timeouts and auto respawns
 */
@SuppressWarnings("unused")
public class FFAArena extends AbstractKitArena {

    private final FFAMode battleMode = (FFAMode) getBattleMode();

    private final Map<Player, Integer> streaks = new HashMap<>();
    private final Cache<UUID, Long> bannedFromArena = CacheBuilder.newBuilder()
            .expireAfterWrite(battleMode.getRemoveForMinutes(), TimeUnit.MINUTES)
            .build();

    public FFAArena(String name) {
        super(BattleModeType.FFA, name);
    }

    public void addPlayer(Player toAdd) {

        Long timestamp = bannedFromArena.getIfPresent(toAdd.getUniqueId());
        if (timestamp != null) {
            long secondsLeft = timestamp - (System.currentTimeMillis() / 1000);
            if (secondsLeft > 0L) {
                MessageBuilder.create(hmb, "modeFFACooldown", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%TIMELEFT%", String.valueOf(secondsLeft))
                        .sendMessage(toAdd);
                return;
            } else bannedFromArena.invalidate(toAdd.getUniqueId());
        }

        pm.addToQueue(battleModeType, toAdd);
        ((HanTeleport) HalfminerSystem.getInstance().getHandler(HandlerType.TELEPORT))
                .startTeleport(toAdd, toAdd.getLocation(), 3,
                        () -> addPlayerInternal(toAdd),
                        () -> pm.setState(BattleState.IDLE, toAdd));
    }

    private void addPlayerInternal(Player toAdd) {
        MessageBuilder.create(hmb, "modeFFAJoined", HalfminerBattle.PREFIX).sendMessage(toAdd);
        addPlayers(toAdd);
        equipPlayers(toAdd);
    }

    public void removePlayer(Player toRemove) {
        restorePlayers(true, toRemove);
        streaks.remove(toRemove);
        if (bannedFromArena.getIfPresent(toRemove.getUniqueId()) == null) {
            pm.setState(BattleState.QUEUE_COOLDOWN, toRemove);
        }
    }

    public void hasDied(Player hasDied) {

        int streakDied = streaks.containsKey(hasDied) ? Math.min(streaks.get(hasDied), 0) - 1 : -1;
        streaks.put(hasDied, streakDied);

        if (-streakDied == battleMode.getRemoveAfterDeaths()) {
            MessageBuilder.create(hmb, "modeFFADiedTooOften", HalfminerBattle.PREFIX).sendMessage(hasDied);
            bannedFromArena.put(hasDied.getUniqueId(), System.currentTimeMillis() / 1000 + battleMode.getRemoveForMinutes() * 60);
            removePlayer(hasDied);
        } else {
            // respawn later
            BukkitScheduler scheduler = hmb.getServer().getScheduler();
            scheduler.runTaskLater(hmb, () -> {
                if (hasDied.isOnline() && pm.getArena(hasDied).equals(this)) {
                    hasDied.spigot().respawn();
                    scheduler.runTaskLater(hmb, () -> teleportIntoArena(hasDied), 1L);
                }
            }, 2L);
        }

        Player killer = hasDied.getKiller();
        if (killer != null) {
            if (!playersInArena.contains(killer)) {
                hmb.getLogger().severe("Killer of " + hasDied.getName() + " " + killer.getName() + " not in Arena");
                return;
            }

            int streak = streaks.containsKey(killer) ? streaks.get(killer) + 1 : 1;
            streaks.put(killer, streak);

            MessageBuilder.create(hmb, "modeFFAKillLog")
                    .addPlaceholderReplace("%ARENA%", getName())
                    .addPlaceholderReplace("%KILLER%", killer.getName())
                    .addPlaceholderReplace("%VICTIM%", hasDied.getName())
                    .addPlaceholderReplace("%KILLSTREAK%", String.valueOf(streak))
                    .addPlaceholderReplace("%DEATHSTREAK%", String.valueOf(Math.abs(streakDied)))
                    .logMessage(Level.INFO);

            try {
                CustomAction action = new CustomAction("ffa-" + streak, hmb.getCacheHolder());
                action.addPlaceholderForNextRun("%TOPLINE%", getCustomLore(killer));
                action.addPlaceholderForNextRun("%BOTTOMLINE%", getCustomLoreID());
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
