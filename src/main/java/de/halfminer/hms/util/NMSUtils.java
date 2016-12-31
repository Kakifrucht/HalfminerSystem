package de.halfminer.hms.util;

import net.minecraft.server.v1_11_R1.*;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

/**
 * Helper methods that access minecraft server / craftbukkit internals.
 * References need to be updated on minecraft updates.
 * Not using reflection, as the code/performance overhead and worsened readability is not worth it.
 */
public final class NMSUtils {

    private NMSUtils() {}

    public static void setLastDamager(Player setFrom, Player setTo) {
        ((CraftPlayer) setFrom).getHandle()
                .damageEntity(DamageSource.playerAttack(((CraftPlayer) setTo).getHandle()), 1f);
    }

    public static int getPing(Player p) {
        return ((CraftPlayer) p).getHandle().ping;
    }

    public static void sendTitlePackets(Player player, String topTitle, String subTitle, int fadeIn, int stay, int fadeOut) {

        if (!player.isOnline() || (topTitle.length() == 0 && subTitle.length() == 0)) return;
        PlayerConnection connection = ((CraftPlayer) player).getHandle().playerConnection;

        connection.sendPacket(new PacketPlayOutTitle(fadeIn, stay, fadeOut));
        if (topTitle.length() > 0)
            connection.sendPacket(new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE,
                    IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + topTitle + "\"}")));

        if (subTitle.length() > 0)
            connection.sendPacket(new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE,
                    IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + subTitle + "\"}")));
    }

    public static void sendActionBarPacket(Player player, String message) {

        if (!player.isOnline() || message.length() == 0) return;

        PacketPlayOutChat actionbar = new PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + message + "\"}"), (byte) 2);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(actionbar);
    }

    public static void sendTablistPackets(Player player, String header, String footer) {

        if (!player.isOnline()) return;

        PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter(
                IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + header + "\"}")
        );

        try {
            Field footerField = packet.getClass().getDeclaredField("b");
            footerField.setAccessible(true);
            footerField.set(packet, IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + footer + "\"}"));
            footerField.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace(); //this should not happen
        }

        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }
}
