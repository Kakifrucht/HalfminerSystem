package de.halfminer.hmb.data;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.enums.GameModeType;
import org.bukkit.entity.Player;

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
     * Check if a player is in queue
     *
     * @param toCheck Player
     * @return true if player is in any queue
     */
    public boolean isInQueue(Player toCheck) {
        return hasState(toCheck, BattleState.IN_QUEUE);
    }

    /**
     * Check if a player is in queue for given gamemode
     *
     * @param gameMode GameModeType
     * @param toCheck Player
     * @return true if player is in queue with given gamemode
     */
    public boolean isInQueue(GameModeType gameMode, Player toCheck) {
        return isInQueue(toCheck) && isInGameMode(toCheck, gameMode);
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
     * Check if a player is in battle
     *
     * @param toCheck Player
     * @return true if player is in battle
     */
    public boolean isInBattle(Player toCheck) {
        return hasState(toCheck, BattleState.IN_BATTLE);
    }

    /**
     * Check if a player is in battle for given gamemode
     * @param gameMode GameModeType
     * @param toCheck Player
     * @return true if player is in battle with given gamemode
     */
    public boolean isInBattle(GameModeType gameMode, Player toCheck) {
        return isInBattle(toCheck) && isInGameMode(toCheck, gameMode);
    }

    private boolean isInGameMode(Player toGet, GameModeType type) {
        return type.equals(getBattlePlayer(toGet).getGameMode());
    }

    /**
     * Adds given players player to queue of given gamemode
     *
     * @param type GameModeType the player is in queue of
     * @param toAdd array of players
     */
    public void addToQueue(GameModeType type, Player... toAdd) {
        for (Player p : toAdd) {
            getBattlePlayer(p).setState(BattleState.IN_QUEUE, type);
        }
    }

    /**
     * Sets the arena the given players are in.
     * This will also set their state to {@link BattleState#IN_BATTLE} and store their state
     *
     * @param toSet Arena the player
     * @param setTo player array to set
     */
    public void setArena(Arena toSet, Player... setTo) {
        for (Player p : setTo) {
            BattlePlayer battlePlayer = getBattlePlayer(p);
            battlePlayer.setState(BattleState.IN_BATTLE);
            battlePlayer.setArena(toSet);
            battlePlayer.storeData();
        }
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
     * Restores the given players inventories
     *
     * @param players array of players to restore
     */
    public void restorePlayerInventory(Player... players) {
        for (Player player : players) {
            getBattlePlayer(player).restoreInventory();
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
     * Returns the first added partner, useful if only one partner is every being set for the gamemode
     *
     * @param get player to get the first partner of
     * @return first set partner or null if none set
     */
    public Player getFirstPartner(Player get) {
        List<BattlePlayer> partners = getBattlePlayer(get).getGamePartners();
        return (partners != null && partners.size() > 0) ? partners.get(0).getBase() : null;
    }
}
