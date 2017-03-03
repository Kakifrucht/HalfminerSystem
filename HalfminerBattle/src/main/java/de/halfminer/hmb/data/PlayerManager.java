package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.enums.BattleState;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Manages the battle database, encapsulating player specific data in {@link BattlePlayer}, used as I/O interface
 */
public class PlayerManager {

    private final Map<UUID, BattlePlayer> playerMap = new HashMap<>();

    public PlayerManager(HalfminerBattle hmb) {
        // cleanup playermap every 20 minutes
        hmb.getServer().getScheduler().runTaskTimer(hmb, () ->
                playerMap.values().removeIf(bp -> bp.getState().equals(BattleState.IDLE)), 24000L, 24000L);
    }

    private BattlePlayer getBattlePlayer(Player player) {

        UUID uuid = player.getUniqueId();
        if (playerMap.containsKey(uuid)) {
            return playerMap.get(uuid);
        } else {
            BattlePlayer newBattlePlayer = new BattlePlayer(player);
            playerMap.put(uuid, newBattlePlayer);
            return newBattlePlayer;
        }
    }

    /**
     * Check if a player is currently busy (in queue, in battle or has cooldown)
     *
     * @param toCheck Player
     * @return true if player is not idle
     */
    public boolean isNotIdle(Player toCheck) {
        return !getBattlePlayer(toCheck).getState().equals(BattleState.IDLE);
    }

    /**
     * Check if a player has global queue cooldown
     *
     * @param toCheck Player
     * @return true if player has queue cooldown
     */
    public boolean hasQueueCooldown(Player toCheck) {
        return hasState(toCheck, BattleState.QUEUE_COOLDOWN);
    }

    /**
     * Check if a player is in queue for given battle mode
     *
     * @param modeType BattleModeType, can be {@link BattleModeType#GLOBAL} to check every mode
     * @param toCheck Player
     * @return true if player is in queue with given battle mode
     */
    public boolean isInQueue(BattleModeType modeType, Player toCheck) {
        return hasState(toCheck, BattleState.IN_QUEUE) && isInBattleMode(toCheck, modeType);
    }

    /**
     * Check if a player is in battle for given battle mode
     *
     * @param modeType BattleModeType, can be {@link BattleModeType#GLOBAL} to check every mode
     * @param toCheck Player
     * @return true if player is in battle with given battle mode
     */
    public boolean isInBattle(BattleModeType modeType, Player toCheck) {
        return hasState(toCheck, BattleState.IN_BATTLE) && isInBattleMode(toCheck, modeType);
    }

    private boolean isInBattleMode(Player toGet, BattleModeType type) {
        BattleModeType modeSet = getBattlePlayer(toGet).getBattleModeType();
        return type.equals(modeSet) || (modeSet != null && type.equals(BattleModeType.GLOBAL));
    }

    /**
     * Adds given players player to queue of given battle mode
     *
     * @param type BattleModeType the player is in queue of
     * @param toAdd array of players
     */
    public void addToQueue(BattleModeType type, Player... toAdd) {
        for (Player p : toAdd) {
            getBattlePlayer(p).setState(BattleState.IN_QUEUE, type);
        }
    }

    /**
     * Sets the arena the given player is in.
     * This will also set his state to {@link BattleState#IN_BATTLE} and store his current Minecraft state.
     *
     * @param setTo player that joined the arena
     * @param wasJoined Arena that was joined
     */
    public void setArena(Player setTo, Arena wasJoined) {
        BattlePlayer battlePlayer = getBattlePlayer(setTo);
        battlePlayer.setArena(wasJoined);
        battlePlayer.storeData();
        battlePlayer.setState(BattleState.IN_BATTLE);
    }

    public Arena getArena(Player player) {
        return getBattlePlayer(player).getArena();
    }

    public void setState(BattleState state, Player... players) {
        for (Player toSet : players) getBattlePlayer(toSet).setState(state);
    }

    private boolean hasState(Player toCheck, BattleState state) {
        return getBattlePlayer(toCheck).getState().equals(state);
    }

    /**
     * This method must be called if a player has disconnected during battle,
     * ensures that even if a player is dead he will be recovered immediately.
     *
     * @param toSet player to set disconnected
     */
    public void setHasDisconnected(Player toSet) {
        getBattlePlayer(toSet).setHasDisconnected();
    }

    /**
     * Check if a ItemStack is a players property or belongs to the arenas kit.
     * This method will only return true if the player is not supposed to fight with his own equipment and the
     * ItemStack in question is his own stuff, for example through a drop mid battle. The item will then be added
     * to the restore list after the battle, however calling functions must also remove the item from the inventory
     * manually if the method returns true. null, {@link Material#GLASS_BOTTLE} and {@link Material#AIR} are never
     * players property
     *
     * @param player Player to check
     * @param toCheck ItemStack in question
     * @return true if ItemStack must be removed from the players inventory
     */
    public boolean checkAndStoreItemStack(Player player, @Nullable ItemStack toCheck) {
        return getBattlePlayer(player).checkAndStoreItemStack(toCheck);
    }

    /**
     * Restores the given players inventories to be used during fight.
     * This will also make {@link #checkAndStoreItemStack(Player, ItemStack)} always return false
     *
     * @param players array of players to restore
     */
    public void restoreInventoryDuringBattle(Player... players) {
        for (Player player : players) {
            if (!hasState(player, BattleState.IN_BATTLE))
                throw new RuntimeException("restoreInventoryDuringBattle() called for " + player.getName() + " while not in battle");
            BattlePlayer battlePlayer = getBattlePlayer(player);
            battlePlayer.setBattleWithOwnEquipment();
            battlePlayer.restoreInventory(player);
        }
    }

    /**
     * Restores the given players location, health, game mode and optionally their inventory.
     * This will also set their state to {@link BattleState#IDLE}
     *
     * @param restoreInventory true if the stored inventory should be recovered
     * @param players array of players to restore
     */
    public void restorePlayers(boolean restoreInventory, Player... players) {
        for (Player player : players) {
            BattlePlayer battlePlayer = getBattlePlayer(player);
            battlePlayer.restorePlayer(restoreInventory);
            battlePlayer.setState(BattleState.IDLE);
        }
    }

    /**
     * Sets the given players battle partners, resetting previously set ones
     *
     * @param toSet player to set the partners off
     * @param partners array of partners to add
     */
    public void setBattlePartners(Player toSet, Player... partners) {

        BattlePlayer toSetBattle = getBattlePlayer(toSet);
        List<BattlePlayer> setTo = new LinkedList<>();
        for (Player p : partners) {
            BattlePlayer add = getBattlePlayer(p);
            if (add.equals(toSetBattle)) continue;
            setTo.add(add);
        }
        toSetBattle.setBattlePartners(setTo);
    }

    /**
     * Returns the first added partner, useful if only one partner is ever being set for the battle mode
     *
     * @param get player to get the first partner of
     * @return first set partner or null if none set
     */
    public Player getFirstPartner(Player get) {
        List<BattlePlayer> partners = getBattlePlayer(get).getGamePartners();
        return (partners != null && partners.size() > 0) ? partners.get(0).getBase() : null;
    }
}
