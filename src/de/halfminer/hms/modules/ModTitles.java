package de.halfminer.hms.modules;

import com.earth2me.essentials.api.UserDoesNotExistException;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.exception.HookException;
import de.halfminer.hms.handlers.HanBossBar;
import de.halfminer.hms.handlers.HanHooks;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Utils;
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
 * - Players online / money
 * - Configurable message
 * - Shows news after delay in bossbar and title
 * - Displays information for new players
 * - Tab titles containing amount of money and playercount
 * - Money through Essentials hook, automatic update
 */
@SuppressWarnings("unused")
public class ModTitles extends HalfminerModule implements Listener {

    private final HanBossBar bossbarHandler = (HanBossBar) hms.getHandler(HandlerType.BOSS_BAR);
    private final HanHooks hooksHandler = (HanHooks) hms.getHandler(HandlerType.HOOKS);
    private final HanTitles titleHandler = (HanTitles) hms.getHandler(HandlerType.TITLES);

    private final Map<String, String> lang = new HashMap<>();
    private final Map<Player, Double> balances = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void joinTitles(PlayerJoinEvent e) {

        final Player joined = e.getPlayer();

        if (!storage.getPlayer(joined).getBoolean(DataType.NEUTP_USED)) {

            titleHandler.sendTitle(joined, Language.placeholderReplace(lang.get("newplayer"),
                    "%PLAYER%", joined.getName()), 10, 200, 10);
            bossbarHandler.sendBar(joined, Language.placeholderReplace(lang.get("newplayerbar"),
                    "%PLAYER%", joined.getName()), BarColor.GREEN, BarStyle.SOLID, 60, 1.0d);

            scheduler.runTaskLater(hms, () -> updateBalanceAndTablist(joined), 3L);
        } else {

            // delay due to potential Essentials issues
            scheduler.runTaskLater(hms, () -> {

                titleHandler.sendTitle(joined, Language.placeholderReplace(lang.get("joinformat"),
                        "%BALANCE%", String.valueOf(updateBalanceAndTablist(joined)),
                        "%PLAYERCOUNT%", getPlayercountString()), 10, 100, 10);

                final String news = storage.getString("news");
                if (news.length() > 0) {
                    scheduler.runTaskLater(hms, () -> {
                        bossbarHandler.sendBar(joined, Language.placeholderReplace(lang.get("news"),
                                "%NEWS%", news), BarColor.YELLOW, BarStyle.SOLID, 30);
                        titleHandler.sendTitle(joined, " \n" + news, 10, 100, 10);
                    }, 120);
                }
            }, 3L);
        }

        updateTablist();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void leaveTitles(PlayerQuitEvent e) {

        balances.remove(e.getPlayer());
        updateTablist();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBalanceChange(UserBalanceUpdateEvent e) {

        if (!e.getPlayer().isOnline()) return;
        balances.put(e.getPlayer(), Utils.roundDouble(e.getNewBalance().doubleValue()));
        updateTablist(e.getPlayer());
    }

    private double updateBalanceAndTablist(final Player player) {

        double balance = Double.MIN_VALUE;

        if (!player.isOnline()) {
            balances.remove(player);
            return balance;
        }

        try {
            balance = hooksHandler.getMoney(player);
        } catch (HookException e) {
            if (e.hasParentException() && e.getParentException() instanceof UserDoesNotExistException) {
                // Try again two ticks later
                scheduler.runTaskLater(hms, () -> updateBalanceAndTablist(player), 2L);
            }
            return balance;
        }

        balances.put(player, balance);
        updateTablist(player);
        return balance;
    }

    private void updateTablist() {
        scheduler.runTaskAsynchronously(hms, () -> balances.keySet().forEach(this::updateTablist));
    }

    private void updateTablist(Player player) {

        titleHandler.setTablistHeaderFooter(player, Language.placeholderReplace(lang.get("tablist"),
                "%BALANCE%", String.valueOf(balances.get(player)), "%PLAYERCOUNT%", getPlayercountString()));
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
        server.getOnlinePlayers().forEach(this::updateBalanceAndTablist);
    }
}
