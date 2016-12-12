package de.halfminer.hms.util;

import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.exception.GiveItemException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * - Parses a CustomtextCache to create a custom ItemStack that will be given to a player
 * - Define custom id, custom name and custom lore, see customitems.txt for example, set quantity
 */
@SuppressWarnings("SameParameterValue")
public class CustomitemCache {

    private final JavaPlugin plugin;
    private final CustomtextCache originalCache;

    public CustomitemCache(JavaPlugin plugin, CustomtextCache textCache) {
        this.plugin = plugin;
        originalCache = textCache;
    }

    public void giveItem(String itemKey, Player giveTo, int amount) throws GiveItemException {

        List<String> itemUnparsed;
        try {
            itemUnparsed = originalCache.getChapter(new String[]{itemKey});
        } catch (CachingException e) {
            e.printStackTrace();
            throw new GiveItemException(GiveItemException.Reason.ITEM_NOT_FOUND);
        }

        ItemStack toGive;

        Material itemMaterial;
        try {
            itemMaterial = Material.valueOf(itemUnparsed.get(0));
        } catch (IllegalArgumentException e) {
            throw new GiveItemException(GiveItemException.Reason.ITEM_SYNTAX_ERROR);
        }

        toGive = new ItemStack(itemMaterial);
        toGive.setAmount(amount);

        for (String parse : itemUnparsed) {
            int indexOf = parse.indexOf(':');
            if (indexOf < 1 || parse.length() == indexOf) continue;

            String key = parse.substring(0, indexOf);
            // get the actual parameter and replace player placeholder
            String parameter = MessageBuilder.create(plugin, parse.substring(indexOf + 1, parse.length()).trim())
                    .setMode(MessageBuilder.Mode.DIRECT_STRING)
                    .addPlaceholderReplace("%PLAYER%", giveTo.getName())
                    .returnMessage();

            switch (key.toLowerCase()) {
                case "itemid":
                    try {
                        short itemId = Short.parseShort(parameter);
                        toGive.setDurability(itemId);
                    } catch (NumberFormatException e) {
                        MessageBuilder.create(plugin, "utilCustomitemCacheInvalidParameter")
                                .addPlaceholderReplace("%KEY%", key)
                                .logMessage(Level.WARNING);
                    }
                    break;
                case "name":
                    Utils.setDisplayName(toGive, ChatColor.translateAlternateColorCodes('&', parameter));
                    break;
                case "lore":
                    Utils.setItemLore(toGive, Arrays.asList(parameter.split("[|]")));
                    break;
                default:
                    MessageBuilder.create(plugin, "utilCustomitemCacheInvalidKey")
                            .addPlaceholderReplace("%KEY%", key)
                            .logMessage(Level.WARNING);
            }
        }

        Map<Integer, ItemStack> notGiven = giveTo.getInventory().addItem(toGive);
        if (notGiven.size() != 0) {
            throw new GiveItemException(notGiven);
        }
    }

    public Set<String> getAllItems() {
        try {
            return originalCache.getAllChapters();
        } catch (CachingException e) {
            return new HashSet<>();
        }
    }
}
