package de.halfminer.hms.util;

import com.earth2me.essentials.Enchantments;
import de.halfminer.hms.exception.CachingException;
import de.halfminer.hms.exception.GiveItemException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.logging.Level;

/**
 * - Parses a CustomtextCache to create a custom ItemStack that will be given to a player
 * - Define custom id, custom name, custom lore and custom enchants, see customitems.txt for example
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

        for (int i = 1; i < itemUnparsed.size(); i++) {

            String parse = itemUnparsed.get(i);
            Pair<String, String> keyParamPair;
            try {
                keyParamPair = Utils.getKeyValuePair(parse);
            } catch (IllegalArgumentException e) {
                logInvalidKey(parse);
                continue;
            }

            // get the actual parameter and replace player placeholder
            String parameter = MessageBuilder.create(plugin, keyParamPair.getRight())
                    .setMode(MessageBuilder.Mode.DIRECT_STRING)
                    .addPlaceholderReplace("%PLAYER%", giveTo.getName())
                    .returnMessage();

            switch (keyParamPair.getLeft().toLowerCase()) {
                case "itemid":
                    try {
                        short itemId = Short.parseShort(parameter);
                        toGive.setDurability(itemId);
                    } catch (NumberFormatException e) {
                        logInvalidParameter(keyParamPair.getLeft(), parameter);
                    }
                    break;
                case "name":
                    Utils.setDisplayName(toGive, ChatColor.translateAlternateColorCodes('&', parameter));
                    break;
                case "lore":
                    Utils.setItemLore(toGive, Arrays.asList(parameter.split("[|]")));
                    break;
                case "enchant":
                    String[] enchantsSplit = parameter.split(" ");
                    for (String enchantStr : enchantsSplit) {
                        String[] split = enchantStr.split(":");
                        if (split.length < 1) {
                            logInvalidParameter(keyParamPair.getLeft(), enchantStr);
                            continue;
                        }

                        Enchantment enchant = Enchantments.getByName(split[0]);
                        if (enchant == null) {
                            logInvalidParameter(keyParamPair.getLeft(), split[0]);
                            continue;
                        }
                        try {
                            int level = Integer.decode(split[1]);
                            toGive.addUnsafeEnchantment(enchant, level);
                        } catch (NumberFormatException e) {
                            logInvalidParameter(keyParamPair.getLeft(), split[1]);
                        }
                    }
                    break;
                default:
                    logInvalidKey(keyParamPair.getLeft());
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

    private void logInvalidKey(String key) {
        MessageBuilder.create(plugin, "utilCustomitemCacheInvalidKey")
                .addPlaceholderReplace("%KEY%", key)
                .logMessage(Level.WARNING);
    }

    private void logInvalidParameter(String key, String param) {
        MessageBuilder.create(plugin, "utilCustomitemCacheInvalidParameter")
                .addPlaceholderReplace("%KEY%", key)
                .addPlaceholderReplace("%PARAMETER%", param)
                .logMessage(Level.WARNING);
    }
}
