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

public class ModStats extends HalfminerModule implements Listener {

    private final Map<Player, Long> timeOnline = new ConcurrentHashMap<>();

    private int timeUntilHomeBlockSeconds;

    public ModStats() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public void playerJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        storage.incrementStatsInt(player, StatsType.JOINS, 1);
        timeOnline.put(player, System.currentTimeMillis() / 1000);

        //Name checking
        String lastName = storage.getStatsString(player, StatsType.LAST_NAME);
        //Called on first join
        if (lastName.length() == 0) storage.setStats(player, StatsType.LAST_NAME, player.getName());
        else if (!lastName.equals(player.getName())) {
            String lastNames = storage.getStatsString(player, StatsType.LAST_NAMES);

            if (lastNames.length() > 0) {
                storage.setStats(player, StatsType.LAST_NAMES, lastNames + ' ' + lastName);
            } else {
                storage.setStats(player, StatsType.LAST_NAMES, lastName);
            }

            hms.getServer().broadcast(Language.getMessagePlaceholderReplace("modStatsNameChange", true,
                    "%PREFIX%", "Name", "%OLDNAME%", lastName, "%NEWNAME%", player.getName()), "hms.default");

            storage.setStats(player, StatsType.LAST_NAME, player.getName());
        }

        storage.set("uid." + player.getName().toLowerCase(), player.getUniqueId().toString());
        storage.setStats(player, StatsType.KD_RATIO, calculateKDRatio(player));

        //Votebarrier setting
        if (storage.getInt("vote." + player.getUniqueId().toString()) == 0) {
            storage.set("vote." + player.getUniqueId().toString(), ((System.currentTimeMillis() / 1000) + timeUntilHomeBlockSeconds));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void playerLeave(PlayerQuitEvent e) {
        setOnlineTime(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void playerDeath(PlayerDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity();
        if (killer != null && victim != killer) {
            int killsKiller = storage.incrementStatsInt(killer, StatsType.KILLS, 1);
            int deathsVictim = storage.incrementStatsInt(victim, StatsType.DEATHS, 1);
            double kdRatioKiller = calculateKDRatio(killer);
            double kdRatioVictim = calculateKDRatio(victim);
            storage.setStats(killer, StatsType.KD_RATIO, kdRatioKiller);
            storage.setStats(victim, StatsType.KD_RATIO, kdRatioVictim);

            killer.sendMessage(Language.getMessagePlaceholderReplace("modStatsPvPKill", true, "%PREFIX%", "PvP",
                    "%VICTIM%", victim.getName(), "%KILLS%", String.valueOf(killsKiller),
                    "%KDRATIO%", String.valueOf(kdRatioKiller)));

            victim.sendMessage(Language.getMessagePlaceholderReplace("modStatsPvPDeath", true, "%PREFIX%", "PvP",
                    "%KILLER%", killer.getName(), "%DEATHS%", String.valueOf(deathsVictim),
                    "%KDRATIO%", String.valueOf(kdRatioVictim)));

            hms.getLogger().info(Language.getMessagePlaceholderReplace("modStatsPvPLog", false,
                    "%KILLER%", killer.getName(), "%VICTIM%", victim.getName()));
        } else {

            storage.incrementStatsInt(victim, StatsType.DEATHS, 1);
            storage.setStats(victim, StatsType.KD_RATIO, calculateKDRatio(victim));

            victim.sendMessage(Language.getMessagePlaceholderReplace("modStatsDeath", true, "%PREFIX%", "PvP",
                    "%DEATHS%", String.valueOf(storage.getStatsInt(victim, StatsType.DEATHS))));

            hms.getLogger().info(Language.getMessagePlaceholderReplace("modStatsDeathLog", false,
                    "%PLAYER%", victim.getName()));
        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onInteract(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof Player) {
            Player clicked = (Player) e.getRightClicked();
            String message = Language.getMessagePlaceholderReplace("modStatsRightClickExempt", true, "%PREFIX%", clicked.getName());
            if (!clicked.hasPermission("hms.bypass.statsrightclick")) {
                String skillgroup = storage.getStatsString(clicked, StatsType.SKILL_GROUP);
                String kills = String.valueOf(storage.getStatsInt(clicked, StatsType.KILLS));
                String kdratio = String.valueOf(storage.getStatsPlayers(clicked, StatsType.KD_RATIO));
                message = Language.getMessagePlaceholderReplace("modStatsRightClick", true, "%PREFIX%", clicked.getName(),
                        "%SKILLGROUP%", skillgroup, "%KILLS%", kills, "%KDRATIO%", kdratio);
            }
            e.getPlayer().sendMessage(message);
            clicked.getWorld().playSound(clicked.getLocation(), Sound.NOTE_STICKS, 1.0f, 1.4f);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void playerMobkill(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Player) && e.getEntity().getKiller() != null)
            storage.incrementStatsInt(e.getEntity().getKiller(), StatsType.MOB_KILLS, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void blockPlace(BlockPlaceEvent e) {
        storage.incrementStatsInt(e.getPlayer(), StatsType.BLOCKS_PLACED, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void blockBreak(BlockBreakEvent e) {
        storage.incrementStatsInt(e.getPlayer(), StatsType.BLOCKS_BROKEN, 1);
    }

    private double calculateKDRatio(Player player) {

        int deaths = storage.getStatsInt(player, StatsType.DEATHS);
        if (deaths == 0) return 999999.99d;
        double calc = storage.getStatsInt(player, StatsType.KILLS) / (double) deaths;
        return Math.round(calc * 100.0d) / 100.0d;
    }

    private void setOnlineTime(Player player) {
        int time;

        if (!timeOnline.containsKey(player)) return; //if reload occurred

        time = (int) ((System.currentTimeMillis() / 1000) - timeOnline.get(player));
        storage.incrementStatsInt(player, StatsType.TIME_ONLINE, time);
        timeOnline.remove(player);
    }

    @Override
    public void reloadConfig() {
        timeUntilHomeBlockSeconds = hms.getConfig().getInt("command.home.timeUntilHomeBlockMinutes") * 60;
    }

    @Override
    public void onDisable() {
        for (Player p : timeOnline.keySet()) setOnlineTime(p);
    }

}
