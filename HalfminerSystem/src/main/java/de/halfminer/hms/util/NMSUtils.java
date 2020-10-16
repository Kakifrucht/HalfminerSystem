package de.halfminer.hms.util;

import net.minecraft.server.v1_12_R1.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * Helper methods that access minecraft server / craftbukkit internals.
 * References need to be updated on minecraft updates.
 * Not using reflection, as the code/performance overhead and worsened readability is not worth it for our uses.
 */
public final class NMSUtils {

    private NMSUtils() {}

    public static void setKiller(Player toKill, Player killer) {
        EntityPlayer toKillNMS = ((CraftPlayer) toKill).getHandle();
        toKillNMS.killer = ((CraftPlayer) killer).getHandle();
        toKillNMS.getCombatTracker()
                .trackDamage(DamageSource.playerAttack(((CraftPlayer) killer).getHandle()), 0.1f, 0.1f);
        /* Preferred method, but may get prevented by third party plugins such as NoCheatPlus: */
        //toKillNMS.damageEntity(DamageSource.playerAttack(((CraftPlayer) killer).getHandle()), Float.MAX_VALUE);
    }

    public static int getPing(Player p) {
        return ((CraftPlayer) p).getHandle().ping;
    }

    public static void sendActionBarPacket(Player player, String message) {

        if (!player.isOnline() || message.length() == 0) return;

        PacketPlayOutChat actionbar = new PacketPlayOutChat(
                IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + message + "\"}"), ChatMessageType.GAME_INFO);
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(actionbar);
    }
}
