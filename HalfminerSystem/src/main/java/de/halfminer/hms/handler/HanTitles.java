package de.halfminer.hms.handler;

import de.halfminer.hms.HalfminerClass;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * - Main title/subtitle
 *   - Send title with delay, to prioritize titles
 * - Actionbar title, where duration can be set
 * - Tablist titles
 */
@SuppressWarnings("SameParameterValue")
public class HanTitles extends HalfminerClass {

    private final Map<Player, BukkitTask> activeActionBarMap = new HashMap<>();


    /**
     * Sends a title to the given player, or broadcasts the title, if player is null.
     * This will show the title with defaults for fadeIn/Out of 10 and 100 ticks stay time.
     *
     * @param player to send the title to, or null to broadcast
     * @param title  message containing the title, color codes do not need to be translated
     */
    public void sendTitle(@Nullable Player player, String title) {
        sendTitle(player, title, 10, 100, 10);
    }

    /**
     * Sends a title to the given player, or broadcasts the title, if player is null.
     * Title can be seperated with newlines, the third line will be displayed as actionbar message.
     * The fadeIn, stay and fadeOut determine the time the title stays, the ActionBar title won't be
     * affected by this.
     *
     * @param player  to send the title to, or null to broadcast
     * @param title   message containing the title, color codes do not need to be translated
     * @param fadeIn  time in ticks until message has fade in
     * @param stay    time in ticks message stays
     * @param fadeOut time in ticks until message faded out after it stay
     */
    public void sendTitle(@Nullable Player player, String title, int fadeIn, int stay, int fadeOut) {

        String[] split = ChatColor.translateAlternateColorCodes('&', title)
                .replace("\\n", "\n")
                .split("\n");

        String topTitle = split[0].isEmpty() ? null : split[0];
        String subTitle = null;
        if (split.length > 1) {
            subTitle = split[1].isEmpty() ? null : split[1];
        }
        if (split.length > 2 && !split[2].isEmpty()) {
            int timeInSeconds = (fadeIn + stay + fadeOut) / 20;
            sendActionBar(player, split[2], Math.max(1, timeInSeconds));
        }

        if (player == null) {
            for (Player sendTo : server.getOnlinePlayers()) {
                sendTo.sendTitle(topTitle, subTitle, fadeIn, stay, fadeOut);
            }
        } else {
            player.sendTitle(topTitle, subTitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Send title via {@link #sendTitle(Player, String, int, int, int)} while adding a delay that can be used
     * to prioritize other titles by adding a delay in ticks, for example after a event listener was called.
     *
     * @param delay ticks to delay
     */
    public void sendTitle(@Nullable Player player, String title, int fadeIn, int stay, int fadeOut, int delay) {
        scheduler.runTaskLater(hms, () -> sendTitle(player, title, fadeIn, stay, fadeOut), delay);
    }

    /**
     * Overload for {@link #sendActionBar(Player, String, int)}, where the time in seconds will be set to 2,
     * as this is the client default.
     *
     * @param player  to send the title to, or null to broadcast
     * @param message message to send
     */
    public void sendActionBar(@Nullable Player player, String message) {
        sendActionBar(player, message, 2);
    }

    /**
     * Sends a actionbar message to a specified player or broadcast, if player is null.
     *
     * @param player  to send the title to, or null to broadcast
     * @param message message to send
     * @param timeSeconds time in seconds to send the title for, must be greater than 0
     */
    public void sendActionBar(@Nullable Player player, String message, int timeSeconds) {

        if (timeSeconds <= 0) {
            throw new IllegalArgumentException("timeSeconds must be greater than 0");
        }

        if (player == null) {
            server.getOnlinePlayers().forEach(sendTo -> addActionBarTask(sendTo, message, timeSeconds));
        } else {
            addActionBarTask(player, message, timeSeconds);
        }
    }

    private void addActionBarTask(Player player, String message, int timeSeconds) {

        if (activeActionBarMap.containsKey(player)) {
            removeAndCancelActionBarTask(player);
        }

        // no need for custom task if the message is empty (just clear) or
        // if it should stay for 2 seconds (client default)
        if (message.isEmpty() || timeSeconds == 2) {
            player.sendActionBar(message.isEmpty() ? " " : message);
            return;
        }

        AtomicInteger atomicTimeLeft = new AtomicInteger(timeSeconds + 1);
        BukkitTask task = scheduler.runTaskTimer(hms, () -> {

            if (!player.isOnline()) {
                removeAndCancelActionBarTask(player);
                return;
            }

            int timeLeft = atomicTimeLeft.decrementAndGet();
            if (timeLeft == 0) {
                player.sendActionBar(" ");
                removeAndCancelActionBarTask(player);
            } else {
                player.sendActionBar('&', message);
            }
        }, 0L, 20L);

        activeActionBarMap.put(player, task);
    }

    private void removeAndCancelActionBarTask(Player player) {
        activeActionBarMap.remove(player).cancel();
    }

    /**
     * Sets the footer and header for a specified player. Seperate header from footer with newline "\n".
     *
     * @param player   to send the title to
     * @param messages message to send
     */
    public void setTablistHeaderFooter(Player player, String messages) {

        String[] messagesParsed = messages.split("%BOTTOM%");
        String header = messagesParsed[0];
        String footer = "";
        if (messagesParsed.length > 1)
            footer = messagesParsed[1];

        player.setPlayerListHeaderFooter(new TextComponent(header), new TextComponent(footer));
    }
}
