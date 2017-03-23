package de.halfminer.hmc.enumerator;

import org.bukkit.Material;

/**
 * Helper class containing Minecraft attackspeed values
 */
@SuppressWarnings("unused")
public enum AttackSpeed {

    WOOD_SWORD      (1.6d),
    STONE_SWORD     (1.6d),
    IRON_SWORD      (1.6d),
    DIAMOND_SWORD   (1.6d),
    GOLD_SWORD      (1.6d),
    WOOD_SHOVEL     (1.0d),
    STONE_SHOVEL    (1.0d),
    IRON_SHOVEL     (1.0d),
    DIAMOND_SHOVEL  (1.0d),
    GOLD_SHOVEL     (1.0d),
    WOOD_PICKAXE    (1.2d),
    STONE_PICKAXE   (1.2d),
    IRON_PICKAXE    (1.2d),
    DIAMOND_PICKAXE (1.2d),
    GOLD_PICKAXE    (1.2d),
    WOOD_HOE        (1.0d),
    STONE_HOE       (2.0d),
    IRON_HOE        (3.0d),
    DIAMOND_HOE     (4.0d),
    GOLD_HOE        (1.0d),
    WOOD_AXE        (0.8d),
    STONE_AXE       (0.8d),
    IRON_AXE        (0.9d),
    DIAMOND_AXE     (1.0d),
    GOLD_AXE        (1.0d);

    final static double defaultSpeed = 4.0d;
    final double speed;

    AttackSpeed(double speed) {
        this.speed = speed;
    }

    public static double getSpeed(Material mat) {

        try {
            return valueOf(mat.toString()).speed;
        } catch (IllegalArgumentException e) {
            return defaultSpeed;
        }
    }

    public static double getDefaultSpeed() {
        return defaultSpeed;
    }
}
