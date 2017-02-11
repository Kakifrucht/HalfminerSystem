package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Arenas containing kits
 */
public abstract class AbstractKitArena extends AbstractArena {

    private ItemStack[] kit;

    protected AbstractKitArena(BattleModeType battleModeType, String name) {
        super(battleModeType, name);
        reload();
    }

    @Override
    public boolean isFree() {
        return super.isFree() && kit != null;
    }

    @Override
    public void reload() {
        kit = am.getKit(battleModeType, getName());
    }

    protected void storeClearAndTeleportPlayers(Player... players) {
        storeAndTeleportPlayers(players);
        parameterToList(players).forEach(p -> p.getInventory().clear());
    }

    protected void equipPlayers(Player... toEquip) {

        for (Player equip : parameterToList(toEquip)) {
            PlayerInventory inv = equip.getInventory();
            inv.setContents(addPlayerInfo(equip, kit));
        }
    }

    private ItemStack[] addPlayerInfo(Player player, ItemStack[] toModify) {
        ItemStack[] modified = new ItemStack[toModify.length];
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(MessageBuilder.create(hmb, "modeGlobalKitArenaCustomLore")
                .addPlaceholderReplace("%ARENA%", getName())
                .addPlaceholderReplace("%MODE%", Utils.makeStringFriendly(battleModeType.toString()))
                .addPlaceholderReplace("%PLAYER%", player.getName()).returnMessage());

        lore.add(ChatColor.DARK_GRAY + "ID: " + ChatColor.DARK_GRAY
                + ChatColor.ITALIC + String.valueOf(System.currentTimeMillis() / 1000));

        for (int i = 0; i < modified.length; i++) {

            if (toModify[i] != null) {
                ItemStack current = toModify[i].clone();

                if (!current.getType().equals(Material.AIR)) {
                    Utils.setItemLore(current, lore);
                }
                modified[i] = current;
            }
        }

        return modified;
    }
}
