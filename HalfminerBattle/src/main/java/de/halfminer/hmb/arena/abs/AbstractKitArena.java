package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Arenas containing kits
 */
public abstract class AbstractKitArena extends AbstractArena {

    private ItemStack[] kit;

    protected AbstractKitArena(GameModeType gameMode, String name) {
        super(gameMode, name);
        reload();
    }

    @Override
    public boolean isActive() {
        return super.isActive() && kit != null;
    }

    @Override
    public void reload() {
        kit = am.getKit(gameMode, getName());
    }

    protected void storeAndClearPlayers(Player... toClear) {

        List<Player> reset = parameterToList(toClear);

        for (Player clear : reset) {
            pm.storePlayerData(clear);
            clear.getInventory().clear();
        }
        teleportIntoArena(reset.toArray(new Player[reset.size()]));
    }

    protected void teleportIntoArena(Player... toTeleport) {

        int spawnNumber = 0;
        for (Player player : parameterToList(toTeleport)) {
            player.teleport(spawns.get(Math.min(spawnNumber++, spawns.size() - 1)));
        }
    }

    protected void equipPlayers(Player... toEquip) {

        for (Player equip : parameterToList(toEquip)) {
            PlayerInventory inv = equip.getInventory();
            inv.setContents(addPlayerInfo(equip, kit));
        }
    }

    protected void restorePlayers(Player... toReset) {

        for (Player resetPlayer : parameterToList(toReset)) {
            pm.restorePlayer(resetPlayer);
        }
    }

    private List<Player> parameterToList(Player... param) {
        return param != null && param.length > 0 ? Arrays.asList(param) : playersInArena;
    }

    private ItemStack[] addPlayerInfo(Player player, ItemStack[] toModify) {
        ItemStack[] modified = new ItemStack[toModify.length];
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GREEN + Utils.makeStringFriendly(getGameMode().toString())
                + " | " + ChatColor.GRAY + player.getName());
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
