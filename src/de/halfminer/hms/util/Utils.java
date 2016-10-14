package de.halfminer.hms.util;

import de.halfminer.hms.HalfminerSystem;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                HalfminerSystem.getInstance().getLogger().warning(Language.getMessagePlaceholders("utilInvalidMaterial",
                        false, "%MATERIAL%", material));
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

    public static TextComponent makeCommandsClickable(String text) {

        TextComponent componentToSend = new TextComponent();
        List<BaseComponent> components = new ArrayList<>();

        for (BaseComponent baseComp : TextComponent.fromLegacyText(text)) {

            TextComponent currentComponent = (TextComponent) baseComp;

            // init new textcomponent with same attributes as original one
            TextComponent currentComp = new TextComponent(currentComponent);
            int currentLowerBound = 0;

            StringBuilder originalText = new StringBuilder(currentComponent.getText());
            boolean readingCommand = false;

            for (int i = 0; i < originalText.length(); i++) {

                if (originalText.charAt(i) == '/') {

                    if (readingCommand) {

                        String command = originalText.substring(currentLowerBound, i);

                        currentComp.setText(command);
                        currentComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                        currentComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new ComponentBuilder(Language.getMessage("cmdCustomtextClick")).create()));
                        // every command is italic
                        currentComp.setItalic(true);
                        // remove closing slash from text
                        originalText.deleteCharAt(i);

                    } else currentComp.setText(originalText.substring(currentLowerBound, i));

                    readingCommand = !readingCommand;
                    components.add(currentComp);
                    currentComp = new TextComponent(currentComponent);
                    currentLowerBound = i;
                }
            }

            // add last component to list
            if (currentLowerBound != originalText.length()) {
                currentComp.setText(originalText.substring(currentLowerBound, originalText.length()));
                components.add(currentComp);
            }
        }

        components.forEach(componentToSend::addExtra);
        return componentToSend;
    }
}
