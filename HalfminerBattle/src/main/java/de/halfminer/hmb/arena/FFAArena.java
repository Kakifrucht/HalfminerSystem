package de.halfminer.hmb.arena;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.mode.FFAMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.handlers.HanBossBar;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.CustomAction;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Free for all arena used by {@link FFAMode}, implementing custom killstreaks, timeouts, scoreboard and auto respawns
 */
@SuppressWarnings("unused")
public class FFAArena extends AbstractKitArena {

    private final FFAMode battleMode = (FFAMode) getBattleMode();

    private final Scoreboard scoreboard = hmb.getServer().getScoreboardManager().getNewScoreboard();
    private final Objective scoreboardObjective;
    private final Team scoreboardTeam;

    private final Map<Player, Integer> streaks = new HashMap<>();
    private final Map<Player, List<PermissionAttachment>> customPermissions = new HashMap<>();
    private final Cache<UUID, Long> bannedFromArena = CacheBuilder.newBuilder()
            .expireAfterWrite(battleMode.getRemoveForMinutes(), TimeUnit.MINUTES)
            .build();
    private final Cache<Player, Boolean> spawnProtection = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .weakKeys()
            .build();

    public FFAArena(String name) {
        super(BattleModeType.FFA, name);

        scoreboardObjective = scoreboard.registerNewObjective("streak", "dummy");
        scoreboardObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        scoreboardObjective.setDisplayName(MessageBuilder.returnMessage("modeFFAScoreboardHeader", hmb, false));
        scoreboardTeam = scoreboard.registerNewTeam("ingame");
        scoreboardTeam.setPrefix(ChatColor.BLUE + "");
    }

    public void addPlayer(Player toAdd) {

        Long timestamp = bannedFromArena.getIfPresent(toAdd.getUniqueId());
        if (timestamp != null) {
            long secondsLeft = timestamp - (System.currentTimeMillis() / 1000);
            if (secondsLeft > 0L) {
                MessageBuilder.create("modeFFACooldown", hmb)
                        .addPlaceholderReplace("%TIMELEFT%", String.valueOf(secondsLeft))
                        .sendMessage(toAdd);
                return;
            } else bannedFromArena.invalidate(toAdd.getUniqueId());
        }

        pm.addToQueue(battleModeType, toAdd);
        battleMode.teleportWithDelay(toAdd, 3,
                () -> addPlayerInternal(toAdd),
                () -> pm.setState(BattleState.IDLE, toAdd));
    }

    private void addPlayerInternal(Player toAdd) {
        addPlayers(toAdd);
        equipPlayers(toAdd);
        addSpawnProtection(toAdd);
        streaks.put(toAdd, 0);

        // add custom bypass permissions
        List<PermissionAttachment> customPermissionsPlayer = new ArrayList<>();
        for (String permission : hmb.getConfig().getStringList("battleMode.ffa.addPermissions")) {
            customPermissionsPlayer.add(toAdd.addAttachment(hmb, permission, true));
        }
        customPermissions.put(toAdd, customPermissionsPlayer);

        toAdd.setScoreboard(scoreboard);
        scoreboardObjective.getScore(toAdd.getName()).setScore(0);
        scoreboardTeam.addEntry(toAdd.getName());

        MessageBuilder.create("modeFFAJoined", hmb)
                .addPlaceholderReplace("%ARENA%", getName())
                .sendMessage(toAdd);
        ((HanTitles) HalfminerSystem.getInstance().getHandler(HandlerType.TITLES))
                .sendTitle(toAdd, MessageBuilder.returnMessage("modeFFAJoinTitle", hmb, false));
    }

    public void removePlayer(Player toRemove) {
        restorePlayers(true, toRemove);
        streaks.remove(toRemove);
        // if not removed due to death ban, add queue cooldown
        if (bannedFromArena.getIfPresent(toRemove.getUniqueId()) == null) {
            pm.setState(BattleState.QUEUE_COOLDOWN, toRemove);
        }
        customPermissions.get(toRemove).forEach(PermissionAttachment::remove);
        customPermissions.remove(toRemove);

        scoreboard.resetScores(toRemove.getName());
        scoreboardTeam.removeEntry(toRemove.getName());
        toRemove.setScoreboard(hmb.getServer().getScoreboardManager().getMainScoreboard());
    }

    public void hasDied(Player hasDied) {

        int streakDied = Math.min(streaks.get(hasDied) - 1, -1);
        streaks.put(hasDied, streakDied);
        scoreboardObjective.getScore(hasDied.getName()).setScore(streakDied);

        if (-streakDied == battleMode.getRemoveAfterDeaths()) {
            MessageBuilder.create("modeFFADiedTooOften", hmb).sendMessage(hasDied);
            bannedFromArena.put(hasDied.getUniqueId(), System.currentTimeMillis() / 1000 + battleMode.getRemoveForMinutes() * 60);
            removePlayer(hasDied);
        } else {
            // respawn with delay to prevent hit delay issues with Minecraft
            BukkitScheduler scheduler = hmb.getServer().getScheduler();
            addSpawnProtection(hasDied);
            scheduler.runTaskLater(hmb, () -> {
                if (hasDied.isOnline() && this.equals(pm.getArena(hasDied))) {
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

            int streak = Math.max(streaks.get(killer) + 1, 1);
            streaks.put(killer, streak);
            scoreboardObjective.getScore(killer.getName()).setScore(streak);

            MessageBuilder.create("modeFFAKillLog", hmb)
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

    public boolean hasSpawnProtection(Player player) {
        return spawnProtection.getIfPresent(player) != null;
    }

    private void addSpawnProtection(Player toProtect) {
        spawnProtection.put(toProtect, true);
        ((HanBossBar) HalfminerSystem.getInstance().getHandler(HandlerType.BOSS_BAR))
                .sendBar(toProtect, MessageBuilder.returnMessage("modeFFASpawnProtectBar", hmb, false),
                        BarColor.GREEN, BarStyle.SOLID, 5);
    }

    @Override
    public boolean forceGameEnd() {
        if (playersInArena.size() > 0) {
            List<Player> inArenaCopy = new ArrayList<>(playersInArena);
            MessageBuilder message = MessageBuilder.create("modeFFAGameEndForced", hmb);
            for (Player player : inArenaCopy) {
                removePlayer(player);
                message.sendMessage(player);
            }
            String allPlayerNames = "";
            for (Player player : inArenaCopy) {
                allPlayerNames += player.getName() + ", ";
            }
            MessageBuilder.create("modeFFAGameEndForcedLog", hmb)
                    .addPlaceholderReplace("%ARENA%", getName())
                    .addPlaceholderReplace("%PLAYERLIST%", allPlayerNames.substring(0, allPlayerNames.length() - 2))
                    .logMessage(Level.INFO);
            return true;
        }
        return false;
    }
}
