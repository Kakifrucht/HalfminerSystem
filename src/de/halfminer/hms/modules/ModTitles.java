package de.halfminer.hms.modules;

import com.earth2me.essentials.api.UserDoesNotExistException;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.StatsType;
import de.halfminer.hms.util.TitleSender;
import net.ess3.api.events.UserBalanceUpdateEvent;
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

public class ModTitles extends HalfminerModule implements Listener {

    private final Map<UUID, Integer> killStreaks = new HashMap<>();
    private final Map<UUID, Integer> deathStreaks = new HashMap<>();

    private final Map<Player, Double> balances = new HashMap<>();

    public ModTitles() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void joinTitles(PlayerJoinEvent e) {

        final Player joined = e.getPlayer();

        // Update tablist titles
        final double balance = updateBalance(joined);
        updateTablist();

        // Show join titles / News
        if (!storage.getStatsBoolean(joined, StatsType.NEUTP_USED)) {
            TitleSender.sendTitle(joined, Language.getMessagePlaceholders("modTitlesNewPlayerFormat", false,
                    "%PLAYER%", joined.getName()), 10, 200, 10);
        } else {
            
            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    TitleSender.sendTitle(joined, Language.getMessagePlaceholders("modTitlesJoinFormat", false,
                            "%BALANCE%", String.valueOf(balance), "%PLAYERCOUNT%", getPlayercountString()), 10, 100, 10);

                    try {
                        Thread.sleep(6000L);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    TitleSender.sendTitle(joined, Language.getMessagePlaceholders("modTitlesNewsFormat",
                            false, "%NEWS%", storage.getString("sys.news")), 40, 120, 40);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void leaveTitles(PlayerQuitEvent e) {

        balances.remove(e.getPlayer());
        updateTablist();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    @SuppressWarnings("unused")
    public void deathActionbarSender(PlayerDeathEvent e) {

        // Show killstreaks in actionbar
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
                TitleSender.sendActionBar(null, Language.getMessagePlaceholders("modTitlesKillStreak", false,
                        "%PLAYER%", killer.getName(), "%STREAK%", String.valueOf(killerStreak)));
            }
            if (victimStreak > 4) {
                TitleSender.sendActionBar(null, Language.getMessagePlaceholders("modTitlesDeathStreak", false,
                        "%PLAYER%", victim.getName(), "%STREAK%", String.valueOf(victimStreak)));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onBalanceChange(UserBalanceUpdateEvent e) {
        updateBalance(e.getPlayer());
    }

    private void updateTablist() {

        hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                for (Player player : balances.keySet()) updateTablist(player);
            }
        });
    }

    private void updateTablist(Player player) {

        TitleSender.setTablistHeaderFooter(player, Language.getMessagePlaceholders("modTitlesTablist",
                false, "%BALANCE%", String.valueOf(balances.get(player)), "%PLAYERCOUNT%", getPlayercountString()));
    }

    private double updateBalance(final Player player) {

        double balance = 0.0d;

        if (!player.isOnline()) {
            balances.remove(player);
            return balance;
        }

        try {
            balance = net.ess3.api.Economy.getMoneyExact(player.getName()).doubleValue();
            balance = Math.round(balance * 100.0d) / 100.0d;
        } catch (UserDoesNotExistException e) {
            // This occurs if player joins for the first time, update it with small delay
            hms.getServer().getScheduler().scheduleSyncDelayedTask(hms, new Runnable() {
                @Override
                public void run() {
                    updateBalance(player);
                }
            }, 2);
        }

        balances.put(player, balance);
        updateTablist(player);
        return balance;
    }

    private String getPlayercountString() {
        return String.valueOf(hms.getServer().getOnlinePlayers().size());
    }

    @Override
    public void reloadConfig() {

        for (Player p : hms.getServer().getOnlinePlayers()) updateBalance(p);
    }
}
