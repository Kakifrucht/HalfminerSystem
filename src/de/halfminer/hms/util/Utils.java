package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import org.bukkit.Material;

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
}
