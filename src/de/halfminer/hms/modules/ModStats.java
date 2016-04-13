package de.halfminer.hms.modules;

import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.ModuleType;
import de.halfminer.hms.interfaces.Disableable;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.HalfminerPlayer;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Pair;
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * - Online time
 * - Last names
 * - Kill/death count
 * - K/D ratio
 * - Blocks placed/broken
 * - Mobkills
 * - View stats on rightclicking a player
 */
@SuppressWarnings("unused")
public class ModStats extends HalfminerModule implements Disableable, Listener, Sweepable {

    private final Map<Player, Long> timeOnline = new ConcurrentHashMap<>();
    private Map<Player, Pair<Player, Long>> lastInteract;

    private int timeUntilHomeBlockSeconds;

    @EventHandler(priority = EventPriority.LOWEST)
    public void joinInitializeStatsAndRename(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        HalfminerPlayer hPlayer = storage.getPlayer(player);
        timeOnline.put(player, System.currentTimeMillis() / 1000);

        String lastName = hPlayer.getString(DataType.LAST_NAME);
        if (!(lastName.length() == 0) && !lastName.equalsIgnoreCase(player.getName())) {

            String lastNames = hPlayer.getString(DataType.LAST_NAMES);

            if (lastNames.length() > 0) {

                // Do not store old name if it was already used
                boolean containsName = false;
                String lastNameLowercase = lastName.toLowerCase();
                for (String str : lastNames.toLowerCase().split(" ")) {
                    if (str.equals(lastNameLowercase)) {
                        containsName = true;
                        break;
                    }
                }
                if (!containsName) hPlayer.set(DataType.LAST_NAMES, lastNames + ' ' + lastName);
            } else {
                hPlayer.set(DataType.LAST_NAMES, lastName);
            }

            server.broadcast(Language.getMessagePlaceholders("modStatsNameChange", true,
                    "%PREFIX%", "Name", "%OLDNAME%", lastName, "%NEWNAME%", player.getName()), "hms.default");
        }

        storage.setUUID(player);
        hPlayer.set(DataType.LAST_NAME, player.getName());

        // Votebarrier setting
        if (storage.getInt("vote." + player.getUniqueId().toString()) == 0) {
            storage.set("vote." + player.getUniqueId().toString(), ((System.currentTimeMillis() / 1000) + timeUntilHomeBlockSeconds));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void updatePlayerTimeLeave(PlayerQuitEvent e) {
        setOnlineTime(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void deathStatsUpdateAndMessage(PlayerDeathEvent e) {

        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity();
        HalfminerPlayer hKiller = storage.getPlayer(killer);
        HalfminerPlayer hVictim = storage.getPlayer(victim);

        if (killer != null && victim != killer) {

            int killsKiller = hKiller.incrementInt(DataType.KILLS, 1);
            int deathsVictim = hVictim.incrementInt(DataType.DEATHS, 1);

            double kdRatioKiller = calculateKDRatio(hKiller);
            double kdRatioVictim = calculateKDRatio(hVictim);
            hKiller.set(DataType.KD_RATIO, kdRatioKiller);
            hVictim.set(DataType.KD_RATIO, kdRatioVictim);

            killer.sendMessage(Language.getMessagePlaceholders("modStatsPvPKill", true, "%PREFIX%", "PvP",
                    "%VICTIM%", victim.getName(), "%KILLS%", String.valueOf(killsKiller),
                    "%KDRATIO%", String.valueOf(kdRatioKiller)));

            victim.sendMessage(Language.getMessagePlaceholders("modStatsPvPDeath", true, "%PREFIX%", "PvP",
                    "%KILLER%", killer.getName(), "%DEATHS%", String.valueOf(deathsVictim),
                    "%KDRATIO%", String.valueOf(kdRatioVictim)));

            hms.getLogger().info(Language.getMessagePlaceholders("modStatsPvPLog", false,
                    "%KILLER%", killer.getName(), "%VICTIM%", victim.getName()));
        } else {

            hVictim.incrementInt(DataType.DEATHS, 1);
            hVictim.set(DataType.KD_RATIO, calculateKDRatio(hVictim));

            victim.sendMessage(Language.getMessagePlaceholders("modStatsDeath", true, "%PREFIX%", "PvP",
                    "%DEATHS%", String.valueOf(hVictim.getInt(DataType.DEATHS))));

            hms.getLogger().info(Language.getMessagePlaceholders("modStatsDeathLog", false,
                    "%PLAYER%", victim.getName()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void interactShowStats(PlayerInteractEntityEvent e) {

        if (!(e.getRightClicked() instanceof Player)) {
            return;
        }

        Player clicker = e.getPlayer();
        Player clicked = (Player) e.getRightClicked();
        long currentTime = System.currentTimeMillis() / 1000;

        if (((ModCombatLog) hms.getModule(ModuleType.COMBAT_LOG)).isTagged(clicker)) return;

        if (lastInteract.containsKey(clicker)) {
            Pair<Player, Long> data = lastInteract.get(clicker);
            if (data.getLeft().equals(clicked) && currentTime < data.getRight()) return;
        }

        String message = Language.getMessagePlaceholders("modStatsRightClickExempt", true,
                "%PREFIX%", clicked.getName());

        if (!clicked.hasPermission("hms.bypass.statsrightclick")) {
            HalfminerPlayer hClicked = storage.getPlayer(clicked);
            String skillgroup = hClicked.getString(DataType.SKILL_GROUP);
            String kills = String.valueOf(hClicked.getInt(DataType.KILLS));
            String kdratio = String.valueOf(hClicked.getDouble(DataType.KD_RATIO));
            message = Language.getMessagePlaceholders("modStatsRightClick", true, "%PREFIX%", clicked.getName(),
                    "%SKILLGROUP%", skillgroup, "%KILLS%", kills, "%KDRATIO%", kdratio);
        }

        clicker.sendMessage(message);
        clicker.playSound(clicked.getLocation(), Sound.BLOCK_SLIME_HIT, 1.0f, 2.0f);

        lastInteract.put(clicker, new Pair<>(clicked, currentTime + 6));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mobkillStats(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Player) && e.getEntity().getKiller() != null)
            storage.getPlayer(e.getEntity().getKiller()).incrementInt(DataType.MOB_KILLS, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockPlaceStats(BlockPlaceEvent e) {
        storage.getPlayer(e.getPlayer()).incrementInt(DataType.BLOCKS_PLACED, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockBreakStats(BlockBreakEvent e) {
        storage.getPlayer(e.getPlayer()).incrementInt(DataType.BLOCKS_BROKEN, 1);
    }

    private double calculateKDRatio(HalfminerPlayer player) {

        int deaths = player.getInt(DataType.DEATHS);
        if (deaths == 0) return 999999.99d;
        double calc = player.getInt(DataType.KILLS) / (double) deaths;
        return Math.round(calc * 100.0d) / 100.0d;
    }

    private void setOnlineTime(Player player) {

        if (!timeOnline.containsKey(player)) return;

        int time = (int) ((System.currentTimeMillis() / 1000) - timeOnline.get(player));
        storage.getPlayer(player).incrementInt(DataType.TIME_ONLINE, time);
        timeOnline.remove(player);
    }

    @Override
    public void loadConfig() {

        lastInteract = new HashMap<>();
        timeUntilHomeBlockSeconds = hms.getConfig().getInt("command.home.timeUntilHomeBlockMinutes") * 60;

        // if reload ocurred while the server ran, add players to list
        if (timeOnline.size() == 0) {
            long time = System.currentTimeMillis() / 1000;
            for (Player player : server.getOnlinePlayers()) {
                timeOnline.put(player, time);
            }
        }

        scheduler.runTaskTimerAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis() / 1000;
                for (Player p : server.getOnlinePlayers()) {
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

    @Override
    public void sweep() {
        this.lastInteract = new HashMap<>();
    }
}
