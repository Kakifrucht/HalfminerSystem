package de.halfminer.hmc.module;

import de.halfminer.hms.handler.hooks.HookException;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import net.ess3.api.UserDoesNotExistException;
import net.ess3.api.events.UserBalanceUpdateEvent;
import org.apache.commons.lang.LocaleUtils;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private Map<Player, Double> balances;
    private NumberFormat numberFormat;


    @EventHandler(priority = EventPriority.MONITOR)
    public void joinTitles(PlayerJoinEvent e) {

        final Player joined = e.getPlayer();
        final HalfminerPlayer hJoined = storage.getPlayer(joined);

        // if player hasn't used newtp and didn't pass 5000 minutes gametime, show newtp info instead of server news
        if (!hJoined.getBoolean(DataType.NEWTP_USED) && hJoined.getInt(DataType.TIME_ONLINE) < 300000) {

            titleHandler.sendTitle(joined, Message.create("modTitlesNewPlayerFormat", hmc)
                    .addPlaceholder("%PLAYER%", joined.getName())
                    .returnMessage(), 10, 200, 10);

            barHandler.sendBar(joined, Message.create("modTitlesNewPlayerFormatBar", hmc)
                    .addPlaceholder("%PLAYER%", joined.getName())
                    .returnMessage(), BarColor.GREEN, BarStyle.SOLID, 60, 1.0d);

            scheduler.runTaskLater(hmc, () -> updateBalanceAndTablist(joined), 3L);
        } else {

            // delay due to potential Essentials issues
            scheduler.runTaskLater(hmc, () -> {

                titleHandler.sendTitle(joined, Message.create("modTitlesJoinFormat", hmc)
                        .addPlaceholder("%BALANCE%", updateBalanceAndTablist(joined))
                        .addPlaceholder("%PLAYERCOUNT%", getPlayercountString())
                        .returnMessage(), 10, 100, 10);

                final String news = coreStorage.getString("news");
                if (news.length() > 0) {
                    scheduler.runTaskLater(hmc, () -> {
                        barHandler.sendBar(joined, Message.create("modTitlesNewsFormat", hmc)
                                .addPlaceholder("%NEWS%", news)
                                .returnMessage(), BarColor.YELLOW, BarStyle.SOLID, 30);
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

    private String updateBalanceAndTablist(final Player player) {

        double balance = Double.MIN_VALUE;

        if (!player.isOnline()) {
            balances.remove(player);
            return getFormattedBalance(balance);
        }

        try {
            balance = hookHandler.getMoney(player);
        } catch (HookException e) {
            if (e.hasParentException() && e.getParentException() instanceof UserDoesNotExistException) {
                // Try again two ticks later
                scheduler.runTaskLater(hmc, () -> updateBalanceAndTablist(player), 2L);
            }
            return getFormattedBalance(balance);
        }

        balances.put(player, balance);
        updateTablist(player);
        return getFormattedBalance(balance);
    }

    private void updateTablist() {
        scheduler.runTaskAsynchronously(hmc, () -> balances.keySet().forEach(this::updateTablist));
    }

    private void updateTablist(Player player) {

        titleHandler.setTablistHeaderFooter(player, Message.create("modTitlesTablist", hmc)
                .addPlaceholder("%BALANCE%", numberFormat.format(balances.get(player)))
                .addPlaceholder("%PLAYERCOUNT%", getPlayercountString())
                .returnMessage());
    }

    private String getPlayercountString() {
        return String.valueOf(server.getOnlinePlayers().size());
    }

    private String getFormattedBalance(double balance) {
        return numberFormat.format(balance);
    }

    @Override
    public void loadConfig() {

        String localeCountryString = hmc.getConfig().getString("titles.numberFormat", "");
        Locale numberFormatLocale = localeCountryString.isEmpty() ? Locale.GERMANY : LocaleUtils.toLocale(localeCountryString);
        this.numberFormat = NumberFormat.getInstance(numberFormatLocale);

        if (balances == null) {
            balances = new ConcurrentHashMap<>();
            server.getOnlinePlayers().forEach(this::updateBalanceAndTablist);
        }
    }
}
