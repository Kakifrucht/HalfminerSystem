package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Static methods that are shared between other classes
 */
public final class Utils {

    private Utils() {}

    public static Set<Material> stringListToMaterialSet(List<String> list) {
        Set<Material> toReturn = new HashSet<>();

        for (String material : list) {
            try {
                toReturn.add(Material.valueOf(material.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                MessageBuilder.create(HalfminerSystem.getInstance(), "utilInvalidMaterial")
                        .addPlaceholderReplace("%MATERIAL%", material)
                        .logMessage(Level.WARNING);
            }
        }
        return toReturn;
    }

    public static boolean hasRoom(Player player, int freeSlots) {

        int freeSlotsCurrent = 0;

        for (ItemStack stack : player.getInventory().getStorageContents())
            if (stack == null)
                freeSlotsCurrent++;

        return freeSlotsCurrent >= freeSlots;
    }

    public static double roundDouble(double toRound) {
        return Math.round(toRound * 100.0d) / 100.0d;
    }

    public static void setDisplayName(ItemStack item, String displayName) {
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
    }
}
