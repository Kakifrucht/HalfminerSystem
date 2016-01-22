package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Records statistics about players in storage
 * - Online time
 * - Last names
 * - Kill/death count
 * - K/D ratio
 * - Blocks placed/broken
 * - Mobkills
 * - View stats on rightclicking a player
 */
public class ModStats extends HalfminerModule implements Listener {

    private final Map<Player, Long> timeOnline = new ConcurrentHashMap<>();

    private int timeUntilHomeBlockSeconds;

    public ModStats() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public void joinInitializeStatsAndRename(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        timeOnline.put(player, System.currentTimeMillis() / 1000);

        String lastName = storage.getStatsString(player, StatsType.LAST_NAME);
        if (!(lastName.length() == 0) && !lastName.equalsIgnoreCase(player.getName())) {

            String lastNames = storage.getStatsString(player, StatsType.LAST_NAMES);

            if (lastNames.length() > 0) {
                //Do not store old name if it was used already
                if (!lastNames.toLowerCase().contains(lastName.toLowerCase())) {
                    storage.setStats(player, StatsType.LAST_NAMES, lastNames + ' ' + lastName);
                }

            } else {
                storage.setStats(player, StatsType.LAST_NAMES, lastName);
            }

            hms.getServer().broadcast(Language.getMessagePlaceholders("modStatsNameChange", true,
                    "%PREFIX%", "Name", "%OLDNAME%", lastName, "%NEWNAME%", player.getName()), "hms.default");
        }

        storage.setUUID(player);
        storage.setStats(player, StatsType.LAST_NAME, player.getName());
        storage.setStats(player, StatsType.KD_RATIO, calculateKDRatio(player));

        //Votebarrier setting
        if (storage.getInt("sys.vote." + player.getUniqueId().toString()) == 0) {
            storage.set("sys.vote." + player.getUniqueId().toString(), ((System.currentTimeMillis() / 1000) + timeUntilHomeBlockSeconds));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void updatePlayerTimeLeave(PlayerQuitEvent e) {
        setOnlineTime(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void deathStatsUpdateAndMessage(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity();
        if (killer != null && victim != killer) {
            int killsKiller = storage.incrementStatsInt(killer, StatsType.KILLS, 1);
            int deathsVictim = storage.incrementStatsInt(victim, StatsType.DEATHS, 1);
            double kdRatioKiller = calculateKDRatio(killer);
            double kdRatioVictim = calculateKDRatio(victim);
            storage.setStats(killer, StatsType.KD_RATIO, kdRatioKiller);
            storage.setStats(victim, StatsType.KD_RATIO, kdRatioVictim);

            killer.sendMessage(Language.getMessagePlaceholders("modStatsPvPKill", true, "%PREFIX%", "PvP",
                    "%VICTIM%", victim.getName(), "%KILLS%", String.valueOf(killsKiller),
                    "%KDRATIO%", String.valueOf(kdRatioKiller)));

            victim.sendMessage(Language.getMessagePlaceholders("modStatsPvPDeath", true, "%PREFIX%", "PvP",
                    "%KILLER%", killer.getName(), "%DEATHS%", String.valueOf(deathsVictim),
                    "%KDRATIO%", String.valueOf(kdRatioVictim)));

            hms.getLogger().info(Language.getMessagePlaceholders("modStatsPvPLog", false,
                    "%KILLER%", killer.getName(), "%VICTIM%", victim.getName()));
        } else {

            storage.incrementStatsInt(victim, StatsType.DEATHS, 1);
            storage.setStats(victim, StatsType.KD_RATIO, calculateKDRatio(victim));

            victim.sendMessage(Language.getMessagePlaceholders("modStatsDeath", true, "%PREFIX%", "PvP",
                    "%DEATHS%", String.valueOf(storage.getStatsInt(victim, StatsType.DEATHS))));

            hms.getLogger().info(Language.getMessagePlaceholders("modStatsDeathLog", false,
                    "%PLAYER%", victim.getName()));
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void interactShowStats(PlayerInteractEntityEvent e) {

        if (e.getRightClicked() instanceof Player) {

            Player clicked = (Player) e.getRightClicked();
            String message = Language.getMessagePlaceholders("modStatsRightClickExempt", true,
                    "%PREFIX%", clicked.getName());

            if (!clicked.hasPermission("hms.bypass.statsrightclick")) {
                String skillgroup = storage.getStatsString(clicked, StatsType.SKILL_GROUP);
                String kills = String.valueOf(storage.getStatsInt(clicked, StatsType.KILLS));
                String kdratio = String.valueOf(storage.getStatsDouble(clicked, StatsType.KD_RATIO));
                message = Language.getMessagePlaceholders("modStatsRightClick", true, "%PREFIX%", clicked.getName(),
                        "%SKILLGROUP%", skillgroup, "%KILLS%", kills, "%KDRATIO%", kdratio);
            }

            e.getPlayer().sendMessage(message);
            e.getPlayer().playSound(clicked.getLocation(), Sound.NOTE_STICKS, 1.0f, 1.4f);

        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void mobkillStats(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Player) && e.getEntity().getKiller() != null)
            storage.incrementStatsInt(e.getEntity().getKiller(), StatsType.MOB_KILLS, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void blockPlaceStats(BlockPlaceEvent e) {
        storage.incrementStatsInt(e.getPlayer(), StatsType.BLOCKS_PLACED, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void blockBreakStats(BlockBreakEvent e) {
        storage.incrementStatsInt(e.getPlayer(), StatsType.BLOCKS_BROKEN, 1);
    }

    private double calculateKDRatio(Player player) {

        int deaths = storage.getStatsInt(player, StatsType.DEATHS);
        if (deaths == 0) return 999999.99d;
        double calc = storage.getStatsInt(player, StatsType.KILLS) / (double) deaths;
        return Math.round(calc * 100.0d) / 100.0d;
    }

    private void setOnlineTime(Player player) {

        if (!timeOnline.containsKey(player)) return;

        int time = (int) ((System.currentTimeMillis() / 1000) - timeOnline.get(player));
        storage.incrementStatsInt(player, StatsType.TIME_ONLINE, time);
        timeOnline.remove(player);
    }

    @Override
    public void reloadConfig() {
        timeUntilHomeBlockSeconds = hms.getConfig().getInt("command.home.timeUntilHomeBlockMinutes") * 60;

        //if reload ocurred while the server ran, add players to list
        if (timeOnline.size() == 0) {
            long time = System.currentTimeMillis() / 1000;
            for (Player player : hms.getServer().getOnlinePlayers()) {
                timeOnline.put(player, time);
            }
        }

        hms.getServer().getScheduler().runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis() / 1000;
                for (Player p : hms.getServer().getOnlinePlayers()) {
                    setOnlineTime(p);
                    timeOnline.put(p, currentTime);
                }
            }
        }, 1200L, 1200L);
    }

    @Override
    public void onDisable() {
        for (Player p : timeOnline.keySet()) setOnlineTime(p);
    }

}
