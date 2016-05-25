package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Static methods that are shared between Modules/handlers/commands
 */
public final class Utils {

    private Utils() {}

    public static Set<Material> stringListToMaterialSet(List<String> list) {
        Set<Material> toReturn = new HashSet<>();

        for (String material : list) {
            try {
                toReturn.add(Material.valueOf(material.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                HalfminerSystem.getInstance().getLogger().warning(Language.getMessagePlaceholders("utilInvalidMaterial",
                        false, "%MATERIAL%", material));
            }
        }
        return toReturn;
    }

    public static boolean hasRoom(Player player, int freeSlots) {

        int freeSlotsCurrent = 0;

        for (ItemStack stack : player.getInventory().getStorageContents())
            if (stack != null)
                freeSlotsCurrent++;

        return freeSlotsCurrent >= freeSlots;
    }
}
