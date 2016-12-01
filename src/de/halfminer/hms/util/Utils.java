package de.halfminer.hms.util;

import com.google.common.cache.Cache;
import de.halfminer.hms.HalfminerSystem;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Static methods that are shared between other classes
 */
@SuppressWarnings("WeakerAccess")
public final class Utils {

    private Utils() {}

    /**
     * Returns a string representation of a location, using block coordinates (only full coords, no floats).
     *
     * @param loc Location to get a String from
     * @return String in the format 'x, y, z'
     */
    public static String getStringFromLocation(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    /**
     * Takes an array and creates the string with spaces between entries, starting from given startIndex.
     *
     * @param array Array to convert
     * @param startIndex int to start from
     * @param translateColor if true, will also translate color codes &
     * @return String
     */
    public static String arrayToString(String[] array, int startIndex, boolean translateColor) {
        return arrayToString(array, startIndex, array.length, translateColor);
    }

    /**
     * Takes an array and creates the string with spaces between entries, starting from given startIndex
     * until given endIndex
     *
     * @param array Array to convert
     * @param startIndex int to start from
     * @param endIndex int to end
     * @param translateColor if true, will also translate color codes &
     * @return String
     */
    public static String arrayToString(String[] array, int startIndex, int endIndex, boolean translateColor) {

        if (!(startIndex <= endIndex)) throw new IllegalArgumentException
                ("startIndex (" + startIndex + ") must be smaller or equal to endIndex(" + endIndex + ")");

        String toReturn = "";
        if (array.length == 0) return toReturn;
        for (int i = startIndex; i < array.length && i < endIndex; i++) {
            toReturn += array[i] + ' ';
        }
        if (translateColor) toReturn = ChatColor.translateAlternateColorCodes('&', toReturn);
        return toReturn.substring(0, toReturn.length() - 1);
    }

    /**
     * Takes a String, lowercases it, exchanges the first character with its uppercase equivalent and transforms
     * underscores to spaces. "SUGAR_CANE" will be "Sugar cane", "tEsT tHiS" would be "Test this".
     *
     * @param str String that should be transformed
     * @return transformed string
     */
    public static String makeStringFriendly(String str) {
        if (str == null || str.length() == 0) return "";
        String toReturn = str.toLowerCase().replace('_', ' ');

        return Character.toUpperCase(toReturn.charAt(0)) + toReturn.substring(1);
    }

    /**
     * Takes a String and filters invalid username characters. Example: "#Notch--" becomes "Notch".
     *
     * @param toFilter String to filter
     * @return filtered and truncated String
     */
    public static String filterNonUsernameChars(String toFilter) {

        StringBuilder sb = new StringBuilder(toFilter.toLowerCase());

        for (int i = 0; i < sb.length(); i++) {

            char toCheck = sb.charAt(i);
            if (Character.isLetter(toCheck) || Character.isDigit(toCheck) || toCheck == '_' || toCheck == ' ') continue;

            sb.deleteCharAt(i);
            i--;
        }

        if (sb.length() > 16) sb.setLength(16);

        return sb.toString();
    }

    /**
     * Gets the playername of a {@link CommandSender}, where it takes the default name if not a player from config file.
     *
     * @param toGet CommandSender to get the name from
     * @return name of CommandSender
     */
    public static String getPlayername(CommandSender toGet) {
        return toGet instanceof Player ?
                toGet.getName() : MessageBuilder.returnMessage(HalfminerSystem.getInstance(), "consoleName");
    }

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

    /**
     * Adds values from old cache to new cache, checks if old cache is null
     *
     * @param oldCache cache to copy from or null
     * @param newCache cache to copy to or pass on if oldCache is null
     * @param <K> key value of both caches
     * @param <V> entry value of both caches
     * @return newCache copied values
     */
    public static <K, V> Cache<K, V> copyValues(@Nullable Cache<K, V> oldCache, Cache<K, V> newCache) {

        if (oldCache != null) {
            oldCache.cleanUp();
            for (Map.Entry<K, V> entry : oldCache.asMap().entrySet()) {
                newCache.put(entry.getKey(), entry.getValue());
            }
        }

        return newCache;
    }
}
