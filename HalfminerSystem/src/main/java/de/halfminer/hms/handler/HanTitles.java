package de.halfminer.hms.handler;

import de.halfminer.hms.HalfminerClass;
import de.halfminer.hms.util.NMSUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

/**
 * - Main title/subtitle
 *   - Send title with delay, to prioritize titles
 * - Actionbar title
 * - Tablist titles
 */
@SuppressWarnings("SameParameterValue")
public class HanTitles extends HalfminerClass {

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

        String topTitle = split[0];
        String subTitle = "";
        if (split.length > 1) {
            subTitle = split[1];
        }
        if (split.length > 2) {
            sendActionBar(player, split[2]);
        }

        if (player == null) {
            for (Player sendTo : server.getOnlinePlayers()) {
                NMSUtils.sendTitlePackets(sendTo, topTitle, subTitle, fadeIn, stay, fadeOut);
            }
        } else {
            NMSUtils.sendTitlePackets(player, topTitle, subTitle, fadeIn, stay, fadeOut);
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
     * Sends a actionbar message to a specified player or broadcast, if player is null.
     *
     * @param player  to send the title to, or null to broadcast
     * @param message message to send
     */
    public void sendActionBar(@Nullable Player player, String message) {

        String send = ChatColor.translateAlternateColorCodes('&', message);
        if (player == null) {
            for (Player sendTo : server.getOnlinePlayers()) {
                NMSUtils.sendActionBarPacket(sendTo, send);
            }
        } else NMSUtils.sendActionBarPacket(player, send);
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

        NMSUtils.sendTablistPackets(player, header, footer);
    }
}
