package de.halfminer.hmb.data;

import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.enums.GameModeType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.*;

/**
 * Manages the battle database, encapsulating player specific data in {@link BattlePlayer}, used as I/O interface
 */
public class PlayerManager implements Listener {

    private final Map<UUID, BattlePlayer> playerMap = new HashMap<>();

    private BattlePlayer getBattlePlayer(Player player) {

        if (playerMap.containsKey(player.getUniqueId())) return playerMap.get(player.getUniqueId());
        else {
            BattlePlayer newBattlePlayer = new BattlePlayer(player);
            playerMap.put(player.getUniqueId(), newBattlePlayer);
            return newBattlePlayer;
        }
    }

    public boolean isNotIdle(Player toCheck) {
        return !getBattlePlayer(toCheck).getState().equals(BattleState.IDLE);
    }

    public boolean isInQueue(Player toCheck) {
        return !isNotIdle(toCheck) && !hasState(toCheck, BattleState.IN_BATTLE);
    }

    public boolean isInQueue(GameModeType gameMode, Player toCheck) {
        return isInQueue(toCheck) && isInGameMode(toCheck, gameMode);
    }

    public boolean isInBattle(Player toCheck) {
        return hasState(toCheck, BattleState.IN_BATTLE);
    }

    public boolean isInBattle(GameModeType gameMode, Player toCheck) {
        return isInBattle(toCheck) && isInGameMode(toCheck, gameMode);
    }

    public boolean isInGameMode(Player toGet, GameModeType type) {
        Arena arena = getBattlePlayer(toGet).getArena();
        return arena != null && arena.getGameMode().equals(type);
    }

    public boolean hasQueueCooldown(Player toCheck) {
        return hasState(toCheck, BattleState.QUEUE_COOLDOWN);
    }

    public void setState(BattleState state, Player... players) {
        for (Player toSet : players) getBattlePlayer(toSet).setState(state);
    }

    private boolean hasState(Player toCheck, BattleState state) {
        return getBattlePlayer(toCheck).getState().equals(state);
    }

    public void storePlayerData(Player player) {
        getBattlePlayer(player).storeData();
    }

    public void restorePlayer(Player player) {
        getBattlePlayer(player).restorePlayer();
    }

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

    public Player getFirstPartner(Player get) {
        List<BattlePlayer> partners = getBattlePlayer(get).getGamePartners();
        return (partners != null && partners.size() > 0) ? partners.get(0).getBase() : null;
    }

    public void setArena(Arena toSet, Player... setTo) {
        for (Player p : setTo) getBattlePlayer(p).setArena(toSet);
    }

    public Arena getArena(Player player) {
        return getBattlePlayer(player).getArena();
    }
}
