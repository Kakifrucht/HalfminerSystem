package de.halfminer.hms.modules;

import com.earth2me.essentials.api.UserDoesNotExistException;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanBossBar;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.Language;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * - Shows join title
 *   - Players online / money
 *   - Configurable message
 *   - Shows news after delay in bossbar and title
 * - Displays information for new players
 * - Tab titles containing amount of money and playercount
 * - Money through Essentials hook, automatic update
 */
@SuppressWarnings("unused")
public class ModTitles extends HalfminerModule implements Listener {

    private final HanTitles titleHandler = (HanTitles) hms.getHandler(HandlerType.TITLES);
    private final HanBossBar bossbarHandler = (HanBossBar) hms.getHandler(HandlerType.BOSSBAR);

    private final Map<String, String> lang = new HashMap<>();
    private final Map<Player, Double> balances = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void joinTitles(PlayerJoinEvent e) {

        final Player joined = e.getPlayer();

        // Update tablist titles
        final double balance = updateBalance(joined);
        updateTablist();

        // Show join titles / news
        if (!storage.getPlayer(joined).getBoolean(DataType.NEUTP_USED)) {
            titleHandler.sendTitle(joined, Language.placeholderReplace(lang.get("newplayer"),
                    "%PLAYER%", joined.getName()), 10, 200, 10);
            bossbarHandler.sendBar(joined, Language.placeholderReplace(lang.get("newplayerbar"),
                    "%PLAYER%", joined.getName()), BarColor.GREEN, BarStyle.SOLID, 60, 1.0d);
        } else {

            titleHandler.sendTitle(joined, Language.placeholderReplace(lang.get("joinformat"),
                    "%BALANCE%", String.valueOf(balance), "%PLAYERCOUNT%", getPlayercountString()), 10, 100, 10);

            final String news = storage.getString("news");
            if (news.length() > 0) {
                scheduler.runTaskLater(hms, new Runnable() {
                    @Override
                    public void run() {
                        bossbarHandler.sendBar(joined, Language.placeholderReplace(lang.get("news"),
                                "%NEWS%", news), BarColor.YELLOW, BarStyle.SOLID, 30);
                        titleHandler.sendTitle(joined, " \n" + news, 10, 100, 10);
                    }
                }, 120);
            }
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

        scheduler.runTaskAsynchronously(hms, new Runnable() {
            @Override
            public void run() {
                for (Player player : balances.keySet()) updateTablist(player);
            }
        });
    }

    private void updateTablist(Player player) {

        titleHandler.setTablistHeaderFooter(player, Language.placeholderReplace(lang.get("tablist"),
                "%BALANCE%", String.valueOf(balances.get(player)), "%PLAYERCOUNT%", getPlayercountString()));
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
            scheduler.runTaskLater(hms, new Runnable() {
                @Override
                public void run() {
                    updateBalance(player);
                }
            }, 2L);
        }

        balances.put(player, balance);
        updateTablist(player);
        return balance;
    }

    private String getPlayercountString() {
        return String.valueOf(server.getOnlinePlayers().size());
    }

    @Override
    public void loadConfig() {

        lang.put("newplayer", Language.getMessage("modTitlesNewPlayerFormat"));
        lang.put("newplayerbar", Language.getMessage("modTitlesNewPlayerFormatBar"));
        lang.put("joinformat", Language.getMessage("modTitlesJoinFormat"));
        lang.put("news", Language.getMessage("modTitlesNewsFormat"));
        lang.put("tablist", Language.getMessage("modTitlesTablist"));
        for (Player p : server.getOnlinePlayers()) updateBalance(p);
    }
}
