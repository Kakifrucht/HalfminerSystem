package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper methods that access minecraft server / craftbukkit internals via reflection.
 * These might break with updates to Minecraft, since accessing them isn't supported by Spigot.
 */
public final class ReflectUtils {

    private static final String NMS_PATH = "net.minecraft.server." + getNMSVersion() + ".";

    private ReflectUtils() {}

    public static void setKiller(Player toKill, Player killer) {

        /* Non reflection code:
        EntityPlayer toKillNMS = ((CraftPlayer) toKill).getHandle();
        toKillNMS.killer = ((CraftPlayer) killer).getHandle();
        toKillNMS.getCombatTracker().trackDamage(DamageSource.playerAttack(((CraftPlayer) killer).getHandle()), .1f, .1f);
        */

        try {
            Class<?> craftPlayerClass = toKill.getClass();
            Object craftPlayerToKill = craftPlayerClass.cast(toKill);
            Object craftPlayerKiller = craftPlayerClass.cast(killer);

            Method getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
            Object entityPlayerToKill = getHandleMethod.invoke(craftPlayerToKill);
            Object entityPlayerKiller = getHandleMethod.invoke(craftPlayerKiller);

            Field killerField = entityPlayerToKill.getClass().getField("killer");
            killerField.set(entityPlayerToKill, entityPlayerKiller);

            Object combatTracker = entityPlayerToKill.getClass().getMethod("getCombatTracker").invoke(entityPlayerToKill);

            Class<?> damageSourceClass = getNMSClass("DamageSource");
            Method damageTrackMethod = combatTracker.getClass().getMethod("trackDamage", damageSourceClass, float.class, float.class);

            Method playerAttackSourceMethod = damageSourceClass.getMethod("playerAttack", getNMSClass("EntityHuman"));
            Object playerAttackSourceObject = playerAttackSourceMethod.invoke(null, entityPlayerKiller);

            damageTrackMethod.invoke(combatTracker, playerAttackSourceObject, .1f, .1f);

        } catch (Exception e) {
            HalfminerSystem.getInstance().getSLF4JLogger()
                    .error("Exception ocurred during setting the killer on player {} to {}", toKill.getName(), killer.getName(), e);
        }
    }

    public static int getPing(Player player) {

        try {
            Method getHandleMethod = player.getClass().getDeclaredMethod("getHandle");

            Object entityPlayer = getHandleMethod.invoke(player);
            Field pingField = entityPlayer.getClass().getDeclaredField("ping");

            return Math.max(pingField.getInt(entityPlayer), 0);
        } catch (Exception e) {
            return -1;
        }
    }

    public static void sendActionBarPacket(Player player, String message) {

        if (!player.isOnline() || message.length() == 0) return;

        try {
            Class<?> packetPlayOutChatClass = getNMSClass("PacketPlayOutChat");
            Class<?> iChatBaseComponentClass = getNMSClass("IChatBaseComponent");
            Class<?> chatMessageTypeClass = getNMSClass("ChatMessageType");

            Constructor<?> packetPlayOutChatConstructor = packetPlayOutChatClass.getConstructor(iChatBaseComponentClass, chatMessageTypeClass);

            Class<?> chatComponentTextClass = getNMSClass("ChatComponentText");
            Object chatComponentText = chatComponentTextClass.getConstructor(String.class).newInstance(message);

            Object chatMessageType = null;
            for (Object obj : chatMessageTypeClass.getEnumConstants()) {
                if (obj.toString().equals("GAME_INFO")) {
                    chatMessageType = obj;
                }
            }

            Object packetPlayOutChatObject = packetPlayOutChatConstructor.newInstance(chatComponentText, chatMessageType);

            Object entityPlayerObject = getEntityPlayerObject(player);
            Object playerConnectionObject = entityPlayerObject.getClass().getDeclaredField("playerConnection").get(entityPlayerObject);
            Method sendPacketMethod = playerConnectionObject.getClass().getMethod("sendPacket", getNMSClass("Packet"));

            sendPacketMethod.invoke(playerConnectionObject, packetPlayOutChatObject);

        } catch (Exception e) {
            HalfminerSystem.getInstance().getSLF4JLogger()
                    .error("Exception ocurred during sending an action bar title to player {}", player.getName(), e);
        }
    }

    private static Object getEntityPlayerObject(Player player)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return player.getClass().getDeclaredMethod("getHandle").invoke(player);
    }

    private static Class<?> getNMSClass(String className) throws ClassNotFoundException {
        return Class.forName(NMS_PATH + className);
    }

    private static String getNMSVersion() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        return packageName.substring(packageName.lastIndexOf(".") + 1);
    }
}
