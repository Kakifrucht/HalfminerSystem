package de.halfminer.hms.modules;

import com.earth2me.essentials.api.UserDoesNotExistException;
import de.halfminer.hms.enums.DataType;
import de.halfminer.hms.exception.HookException;
import de.halfminer.hms.util.MessageBuilder;
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

    private final Map<Player, Double> balances = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void joinTitles(PlayerJoinEvent e) {

        final Player joined = e.getPlayer();

        if (!storage.getPlayer(joined).getBoolean(DataType.NEUTP_USED)) {

            titleHandler.sendTitle(joined, MessageBuilder.create(hms, "modTitlesNewPlayerFormat")
                    .addPlaceholderReplace("%PLAYER%", joined.getName())
                    .returnMessage(), 10, 200, 10);

            barHandler.sendBar(joined, MessageBuilder.create(hms, "modTitlesNewPlayerFormatBar")
                    .addPlaceholderReplace("%PLAYER%", joined.getName())
                    .returnMessage(), BarColor.GREEN, BarStyle.SOLID, 60, 1.0d);

            scheduler.runTaskLater(hms, () -> updateBalanceAndTablist(joined), 3L);
        } else {

            // delay due to potential Essentials issues
            scheduler.runTaskLater(hms, () -> {

                titleHandler.sendTitle(joined, MessageBuilder.create(hms, "modTitlesJoinFormat")
                        .addPlaceholderReplace("%BALANCE%", String.valueOf(updateBalanceAndTablist(joined)))
                        .addPlaceholderReplace("%PLAYERCOUNT%", getPlayercountString())
                        .returnMessage(), 10, 100, 10);

                final String news = storage.getString("news");
                if (news.length() > 0) {
                    scheduler.runTaskLater(hms, () -> {
                        barHandler.sendBar(joined, MessageBuilder.create(hms, "modTitlesNewsFormat")
                                .addPlaceholderReplace("%NEWS%", news)
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

    private double updateBalanceAndTablist(final Player player) {

        double balance = Double.MIN_VALUE;

        if (!player.isOnline()) {
            balances.remove(player);
            return balance;
        }

        try {
            balance = hookHandler.getMoney(player);
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

        titleHandler.setTablistHeaderFooter(player, MessageBuilder.create(hms, "modTitlesTablist")
                .addPlaceholderReplace("%BALANCE%", String.valueOf(balances.get(player)))
                .addPlaceholderReplace("%PLAYERCOUNT%", getPlayercountString())
                .returnMessage());
    }

    private String getPlayercountString() {
        return String.valueOf(server.getOnlinePlayers().size());
    }

    @Override
    public void loadConfig() {
        server.getOnlinePlayers().forEach(this::updateBalanceAndTablist);
    }
}
