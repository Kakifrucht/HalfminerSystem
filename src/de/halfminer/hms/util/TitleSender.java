package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class TitleSender {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();

    /**
     * Sends a title to the given player, or broadcasts the title, if player is null.
     * This will show the title with defaults for fadeIn/Out of 10 and 100 ticks stay time.
     *
     * @param player to send the title to, or null to broadcast
     * @param title  message containing the title, color codes do not need to be translated
     */
    public static void sendTitle(Player player, String title) {
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
    public static void sendTitle(Player player, String title, int fadeIn, int stay, int fadeOut) {

        String[] split = ChatColor.translateAlternateColorCodes('&', title).replace("\\n", "\n").split("\n");
        String topTitle = split[0];
        String subTitle = "";
        if (split.length > 1) subTitle = split[1];
        if (split.length > 2) sendActionBar(player, split[2]);

        if (player == null) {

            for (Player sendTo : hms.getServer().getOnlinePlayers()) {
                sendTitlePackets(sendTo, topTitle, subTitle, fadeIn, stay, fadeOut);
            }

        } else {
            sendTitlePackets(player, topTitle, subTitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Sends a actionbar message to a specified player or broadcast, if player is null.
     *
     * @param player  to send the title to, or null to broadcast
     * @param message message to send
     */
    public static void sendActionBar(Player player, String message) {

        String send = ChatColor.translateAlternateColorCodes('&', message);
        if (player == null) {

            for (Player sendTo : hms.getServer().getOnlinePlayers()) sendActionBarPacket(sendTo, send);
        } else {

            sendActionBarPacket(player, send);
        }
    }

    /**
     * Sets the footer and header for a specified player. Seperate header from footer with newline
     *
     * @param player   to send the title to
     * @param messages message to send
     */
    public static void setTablistHeaderFooter(Player player, String messages) {

        if (!player.isOnline()) return;
        String[] messagesParsed = messages.split("%BOTTOM%");
        String header = messagesParsed[0];
        String footer = "";
        if (messagesParsed.length > 1) footer = messagesParsed[1];

        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(
                IChatBaseComponent.ChatSerializer.a("{'text': '" + header + "'}")
        );

        try {
            Field footerField = packet.getClass().getDeclaredField("b");
            footerField.setAccessible(true);
            footerField.set(packet, IChatBaseComponent.ChatSerializer.a("{'text': '" + footer + "'}"));
            footerField.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace(); //this should not happen
        }

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    private static void sendTitlePackets(Player player, String topTitle, String subTitle, int fadeIn, int stay, int fadeOut) {

        if (!player.isOnline() || (topTitle.length() == 0 && subTitle.length() == 0)) return;
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        connection.sendPacket(new PacketPlayOutTitle(fadeIn, stay, fadeOut));
        if (topTitle.length() > 0)
            connection.sendPacket(new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                    IChatBaseComponent.ChatSerializer.a("{'text': '" + topTitle + "'}")));

        if (subTitle.length() > 0)
            connection.sendPacket(new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a("{'text': '" + subTitle + "'}")));
    }

    private static void sendActionBarPacket(Player player, String message) {

        if (!player.isOnline() || message.length() == 0) return;

        PacketPlayOutChat actionbar = new PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a("{'text': '" + message + "'}"), (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(actionbar);
    }

}
