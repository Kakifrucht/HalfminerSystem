package de.halfminer.hms.enums;

import org.bukkit.Material;

/**
 * Contains items that should be sold, including their command clear text and their itemid/durability
 */
public enum Sellable {

    BEETROOT        (Material.BEETROOT, 0, "beetroot"),
    BROWN_MUSHROOM  (Material.BROWN_MUSHROOM, 0, "brownmushroom"),
    CACTUS          (Material.CACTUS, 0, "cactus"),
    CARROT_ITEM     (Material.CARROT_ITEM, 0, "carrot"),
    INK_SACK        (Material.INK_SACK, 3, "cocoa"),
    MELON           (Material.MELON, 0, "melon"),
    NETHER_STALK    (Material.NETHER_STALK, 0, "netherwart"),
    POTATO_ITEM     (Material.POTATO_ITEM, 0, "potato"),
    PUMPKIN         (Material.PUMPKIN, 0, "pumpkin"),
    RED_MUSHROOM    (Material.RED_MUSHROOM, 0, "redmushroom"),
    SUGAR_CANE      (Material.SUGAR_CANE, 0, "sugarcane"),
    WHEAT           (Material.WHEAT, 0, "wheat");

    private final Material material;
    private final int itemId;
    private final String clearText;

    Sellable(Material material, int itemId, String clearText) {
        this.material = material;
        this.itemId = itemId;
        this.clearText = clearText;
    }

    public Material getMaterial() {
        return material;
    }

    public int getItemId() {
        return itemId;
    }

    public String getClearText() {
        return clearText;
    }

    public static Sellable getFromMaterial(Material material) {
        return valueOf(material.toString());
    }

    public static Sellable getFromString(String string) {

        for (Sellable sellable : values()) {
            if (sellable.getClearText().equalsIgnoreCase(string))
                return sellable;
        }
        return null;
    }
}
