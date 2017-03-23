package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.BattleClass;
import de.halfminer.hmb.mode.abs.BattleModeType;
import de.halfminer.hmb.mode.GlobalMode;
import de.halfminer.hmb.mode.abs.BattleMode;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;

import java.util.*;

/**
 * Abstract arena implementing all in interfaces {@link Arena} defined methods
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractArena extends BattleClass implements Arena {

    protected final BattleModeType battleModeType;
    protected final String name;
    protected List<Location> spawns = new ArrayList<>();
    private ItemStack[] kit;

    protected final LinkedList<Player> playersInArena = new LinkedList<>();

    protected AbstractArena(BattleModeType battleModeType, String name) {
        this.battleModeType = battleModeType;
        this.name = name;
    }

    @Override
    public boolean isFree() {
        return !spawns.isEmpty() && kit != null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getPlayerCount() {
        return playersInArena.size();
    }

    @Override
    public BattleModeType getBattleModeType() {
        return battleModeType;
    }

    @Override
    public List<Player> getPlayersInArena() {
        return playersInArena;
    }

    @Override
    public void setSpawns(List<Location> newSpawns) {
        this.spawns = new ArrayList<>(newSpawns);
    }

    @Override
    public void setSpawn(Location spawn, int spawnNumber) {
        if (spawnNumber < 0 || spawnNumber > spawns.size() - 1)
            spawns.add(spawn);
        else spawns.set(spawnNumber, spawn);
    }

    @Override
    public void removeSpawn(int spawnNumber) {
        if (spawnNumber >= 0 && spawnNumber < spawns.size())
            spawns.remove(spawnNumber);
        else spawns.clear();
    }

    @Override
    public List<Location> getSpawns() {
        return spawns;
    }

    @Override
    public void setKit(ItemStack[] kit) {
        this.kit = kit;
    }

    @Override
    public ItemStack[] getKit() {
        return kit;
    }

    @Override
    public boolean isCloseToSpawn(Location loc) {

        GlobalMode global = (GlobalMode) hmb.getBattleMode(BattleModeType.GLOBAL);
        for (Location spawn : spawns) {
            if (spawn.getWorld().equals(loc.getWorld()) && spawn.distance(loc) <= global.getTeleportSpawnDistance())
                return true;
        }

        return false;
    }

    /**
     * Add the specified players to the arena, teleports and heals them while setting their GameMode to <i>ADVENTURE</i>
     *
     * @param players to be added to the arena
     */
    protected void addPlayers(Player... players) {
        if (!isFree()) throw new RuntimeException("Tried to add players to an occupied arena");

        for (Player toAdd : parameterToList(players)) {
            playersInArena.add(toAdd);
            pm.setArena(toAdd, this);
            // heal
            toAdd.setHealth(20.0d);
            toAdd.setFoodLevel(20);
            toAdd.setSaturation(10);
            toAdd.setExhaustion(0F);
            toAdd.setFireTicks(0);
            for (PotionEffect effect : toAdd.getActivePotionEffects())
                toAdd.removePotionEffect(effect.getType());
            toAdd.setGameMode(GameMode.ADVENTURE);
            toAdd.getInventory().clear();
        }
        teleportIntoArena(parameterToArray(players));
    }

    protected void teleportIntoArena(Player... toTeleport) {

        List<Player> toTeleportList = parameterToList(toTeleport);
        if (toTeleportList.size() > 1) {
            int spawnNumber = 0;
            for (Player player : toTeleportList) {
                if (!player.teleport(spawns.get(Math.min(spawnNumber++, spawns.size() - 1)))) {
                    hmb.getLogger().warning("Player " + player.getName() + " could not be teleported into the arena");
                }
            }
        } else {
            toTeleportList.get(0).teleport(spawns.get(new Random().nextInt(spawns.size())));
        }
    }

    /**
     * Equips the players with either the arenas kit with added battle branding or with players own stuff
     *
     * @param useKit true to use the arenas kit, false to use the players inventory
     * @param toEquip player to equip
     */
    protected void equipPlayer(boolean useKit, Player toEquip) {
        if (useKit) {
            PlayerInventory inv = toEquip.getInventory();
            toEquip.closeInventory();
            inv.setContents(addPlayerInfo(toEquip, kit));
        } else {
            pm.restoreInventoryDuringBattle(toEquip);
        }
        toEquip.playSound(toEquip.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.6f);
    }

    private ItemStack[] addPlayerInfo(Player player, ItemStack[] toModify) {
        ItemStack[] modified = new ItemStack[toModify.length];
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(getCustomLore(player));
        lore.add(getCustomLoreID());

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

    /**
     * Restores given players location/inventory/state before he entered the arena while removing them from the arena
     *
     * @param players players to restore
     */
    protected void restorePlayers(Player... players) {
        pm.restorePlayers(parameterToArray(players));
        for (Player player : parameterToArray(players)) {
            playersInArena.remove(player);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 1.6f);
        }
    }

    protected BattleMode getBattleMode() {
        return hmb.getBattleMode(battleModeType);
    }

    protected String getCustomLore(Player player) {
        return MessageBuilder.create("modeGlobalKitArenaCustomLore", hmb)
                .togglePrefix()
                .addPlaceholderReplace("%ARENA%", getName())
                .addPlaceholderReplace("%MODE%", Utils.makeStringFriendly(battleModeType.toString()))
                .addPlaceholderReplace("%PLAYER%", player.getName()).returnMessage();
    }

    protected String getCustomLoreID() {
        return ChatColor.DARK_GRAY + "ID: " + ChatColor.DARK_GRAY
                + ChatColor.ITALIC + String.valueOf(System.currentTimeMillis() / 1000);
    }

    protected List<Player> parameterToList(Player... param) {
        return param != null && param.length > 0 ? Arrays.asList(param) : playersInArena;
    }

    protected Player[] parameterToArray(Player... param) {
        return param != null && param.length > 0 ? param : playersInArena.toArray(new Player[playersInArena.size()]);
    }
}
