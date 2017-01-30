package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.enums.GameModeType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Arenas containing kits
 */
public abstract class AbstractKitArena extends AbstractArena {

    private ItemStack[] kitArmor;
    private ItemStack[] kitContent;
    private ItemStack[] kitExtra;

    protected AbstractKitArena(GameModeType gameMode, String name) {
        super(gameMode, name);
        reload();
    }

    @Override
    public boolean isActive() {
        return super.isActive() && kitArmor != null;
    }

    @Override
    public void reload() {
        ItemStack[][] kit = am.getKit(gameMode, getName());
        if (kit == null) return;
        this.kitArmor = kit[0];
        this.kitContent = kit[1];
        this.kitExtra = kit[2];
    }

    protected void clearAndStorePlayers(Player... toClear) {

        List<Player> reset = toClear != null ? Arrays.asList(toClear) : playersInArena;

        int spawnNumber = 0;
        for (Player clear : reset) {
            clear.leaveVehicle();
            pm.storePlayerData(clear);
            clear.teleport(spawns.get(Math.min(spawnNumber++, spawns.size() - 1)));
        }
    }

    protected void equipPlayers(Player... toEquip) {

        List<Player> reset = toEquip != null ? Arrays.asList(toEquip) : playersInArena;

        for (Player equip : reset) {
            PlayerInventory inv = equip.getInventory();
            inv.setArmorContents(addPlayerInfo(equip, kitArmor));
            inv.setContents(addPlayerInfo(equip, kitContent));
            inv.setExtraContents(addPlayerInfo(equip, kitExtra));
        }
    }

    protected void restorePlayers(Player... toReset) {

        List<Player> reset = toReset != null ? Arrays.asList(toReset) : playersInArena;

        for (Player resetPlayer : reset) {
            pm.restorePlayer(resetPlayer);
        }
    }

    private ItemStack[] addPlayerInfo(Player player, ItemStack[] toModify) {
        ItemStack[] modified = new ItemStack[toModify.length];
        List<String> lore = Arrays.asList("", ChatColor.GREEN + player.getName(),
                ChatColor.GRAY.toString() + ChatColor.ITALIC + String.valueOf(System.currentTimeMillis() / 1000));

        for (int i = 0; i < modified.length; i++) {

            if (toModify[i] != null) {
                ItemStack current = toModify[i].clone();

                if (!current.getType().equals(Material.AIR)) {
                    ItemMeta meta = current.getItemMeta();
                    meta.setLore(lore);
                    current.setItemMeta(meta);
                }
                modified[i] = current;
            }
        }

        return modified;
    }
}
