package de.halfminer.hms.modules;

import com.earth2me.essentials.api.UserDoesNotExistException;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.enums.StatsType;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.Language;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Add titles containing information to the game
 * - Join titles to show basic information (playercount, currency, commands, news)
 * - Shows servernews
 * - Display information for new players
 * - Tab titles containing playercount and currency of the player
 */
@SuppressWarnings("unused")
public class ModTitles extends HalfminerModule implements Listener {

    private final HanTitles titleHandler = (HanTitles) hms.getHandler(HandlerType.TITLES);

    private final Map<Player, Double> balances = new HashMap<>();

    public ModTitles() {
        reloadConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void joinTitles(PlayerJoinEvent e) {

        final Player joined = e.getPlayer();

        // Update tablist titles
        final double balance = updateBalance(joined);
        updateTablist();

        // Show join titles / news
        if (!storage.getStatsBoolean(joined, StatsType.NEUTP_USED)) {
            titleHandler.sendTitle(joined, Language.getMessagePlaceholders("modTitlesNewPlayerFormat", false,
                    "%PLAYER%", joined.getName()), 10, 200, 10);
        } else {
            
            hms.getServer().getScheduler().runTaskAsynchronously(hms, new Runnable() {
                @Override
                public void run() {
                    titleHandler.sendTitle(joined, Language.getMessagePlaceholders("modTitlesJoinFormat", false,
                            "%BALANCE%", String.valueOf(balance), "%PLAYERCOUNT%", getPlayercountString()), 10, 100, 10);

                    try {
                        Thread.sleep(6000L);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }

                    titleHandler.sendTitle(joined, Language.getMessagePlaceholders("modTitlesNewsFormat",
                            false, "%NEWS%", storage.getString("sys.news")), 40, 120, 40);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void leaveTitles(PlayerQuitEvent e) {

        balances.remove(e.getPlayer());
        updateTablist();
    }



    @EventHandler(ignoreCancelled = true)
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

        titleHandler.setTablistHeaderFooter(player, Language.getMessagePlaceholders("modTitlesTablist",
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
