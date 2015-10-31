package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ModTitles extends HalfminerModule implements Listener {

    private final Map<UUID, Integer> killStreaks = new HashMap<>();
    private final Map<UUID, Integer> deathStreaks = new HashMap<>();

    private final Map<Player, Double> balances = new ConcurrentHashMap<>();
    private int playercount = hms.getServer().getOnlinePlayers().size();


    public ModTitles() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void joinTitles(PlayerJoinEvent e) {

        playercount++;

        final Player joined = e.getPlayer();
        int timeOnline = storage.getStatsInt(joined, StatsType.TIME_ONLINE);

        if (timeOnline == 0) TitleSender.sendTitle(joined, Language.getMessage("modStaticListenersNewPlayerFormat"));
        else {

            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    TitleSender.sendTitle(joined, Language.getMessagePlaceholderReplace("modStaticListenersJoinFormat",
                            false, "%NEWS%", storage.getString("sys.news")));

                    try {
                        Thread.sleep(6000l);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    TitleSender.sendTitle(joined, Language.getMessagePlaceholderReplace("modStaticListenersNewsFormat",
                            false, "%NEWS%", storage.getString("sys.news")), 40, 180, 40);
                }
            });
        }
        updateTablists();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void leaveCountUpdate(PlayerQuitEvent e) {
        playercount--;
        updateTablists();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void deathActionbarSender(PlayerDeathEvent e) {

        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer != null) {

            UUID killerUid = killer.getUniqueId();
            UUID victimUid = victim.getUniqueId();

            deathStreaks.remove(killerUid);
            killStreaks.remove(victimUid);

            int killerStreak;
            int victimStreak;

            if (killStreaks.containsKey(killerUid)) killerStreak = killStreaks.get(killerUid) + 1;
            else killerStreak = 1;

            if (deathStreaks.containsKey(victimUid)) victimStreak = deathStreaks.get(victimUid) + 1;
            else victimStreak = 1;

            killStreaks.put(killerUid, killerStreak);
            deathStreaks.put(victimUid, victimStreak);

            if (killerStreak > 4) {
                TitleSender.sendActionBar(null, Language.getMessagePlaceholderReplace("modTitlesKillStreak", false,
                        "%PLAYER%", killer.getName(), "%STREAK%", String.valueOf(killerStreak)));
            }
            if (victimStreak > 4) {
                TitleSender.sendActionBar(null, Language.getMessagePlaceholderReplace("modTitlesDeathStreak", false,
                        "%PLAYER%", victim.getName(), "%STREAK%", String.valueOf(victimStreak)));
            }
        }
    }

    private void updateTablists() {
        hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<Player, Double> entry : balances.entrySet()) {
                    double balance = entry.getValue();
                    balance = Math.round(balance * 100.0d) / 100.0d;

                    TitleSender.setTablistHeaderFooter(entry.getKey(), Language.getMessagePlaceholderReplace("modTitlesTablist",
                            false, "%BALANCE%", String.valueOf(balance), "%PLAYERCOUNT%", String.valueOf(playercount)));
                }
            }
        });
    }

    @Override
    public void reloadConfig() {
        hms.getServer().getScheduler().scheduleSyncRepeatingTask(hms, new Runnable() {
            @Override
            public void run() {
                Economy econ = HalfminerSystem.getEconomy();
                balances.clear();
                if (econ != null) {
                    for (Player player : hms.getServer().getOnlinePlayers()) {
                        balances.put(player, econ.getBalance(player));
                    }
                } else {
                    for (Player player : hms.getServer().getOnlinePlayers()) {
                        balances.put(player, 0.0d);
                    }
                }
                updateTablists();
            }
        }, 0, 100);

        //TODO read config animations titles.yml
    }
}
