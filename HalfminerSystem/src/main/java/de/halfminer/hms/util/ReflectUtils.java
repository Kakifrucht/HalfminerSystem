package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;

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
            Object entityPlayerToKill = getEntityPlayerObject(toKill);
            Object entityPlayerKiller = getEntityPlayerObject(killer);

            Field killerField = entityPlayerToKill.getClass().getField("killer");
            killerField.set(entityPlayerToKill, entityPlayerKiller);

            Object combatTracker = entityPlayerToKill.getClass().getMethod("getCombatTracker").invoke(entityPlayerToKill);

            Class<?> damageSourceClass = getNMSClass("DamageSource");
            Method damageTrackMethod = combatTracker.getClass().getMethod("trackDamage", damageSourceClass, float.class, float.class);

            Method playerAttackSourceMethod = damageSourceClass.getMethod("playerAttack", getNMSClass("EntityHuman"));
            Object playerAttackSourceObject = playerAttackSourceMethod.invoke(null, entityPlayerKiller);

            damageTrackMethod.invoke(combatTracker, playerAttackSourceObject, .1f, .1f);

        } catch (Exception e) {
            HalfminerSystem.getInstance().getLogger()
                    .log(Level.SEVERE, "Exception ocurred during setting the killer on player " + toKill.getName() + " to " + killer.getName(), e);
        }
    }

    public static int getPing(Player player) {

        try {
            Object entityPlayer = getEntityPlayerObject(player);
            Field pingField = entityPlayer.getClass().getDeclaredField("ping");

            return Math.max(pingField.getInt(entityPlayer), 0);
        } catch (Exception e) {
            return -1;
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
