package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.mode.GlobalMode;
import de.halfminer.hmb.mode.abs.BattleMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.*;

/**
 * Abstract arena implementing all in interfaces {@link Arena} defined methods
 */
@SuppressWarnings("WeakerAccess")
public abstract class AbstractArena implements Arena {

    protected static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    protected static final PlayerManager pm = hmb.getPlayerManager();
    protected static final ArenaManager am = hmb.getArenaManager();

    // Arena state
    protected final BattleModeType battleModeType;
    protected final String name;
    protected List<Location> spawns = new ArrayList<>();
    protected final LinkedList<Player> playersInArena = new LinkedList<>();

    protected AbstractArena(BattleModeType battleModeType, String name) {
        this.battleModeType = battleModeType;
        this.name = name;
    }

    @Override
    public boolean isFree() {
        return !spawns.isEmpty();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BattleModeType getBattleModeType() {
        return battleModeType;
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
    public void addPlayers(Player... players) {
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
        }
        teleportIntoArena(parameterToArray(players));
    }

    protected void teleportIntoArena(Player... toTeleport) {

        int spawnNumber = 0;
        for (Player player : parameterToList(toTeleport)) {
            if (!player.teleport(spawns.get(Math.min(spawnNumber++, spawns.size() - 1)))) {
                hmb.getLogger().warning("Player " + player.getName() + " could not be teleported into the arena");
            }
        }
    }

    /**
     * Restores given players location/inventory/state before he entered the arena while removing them from the arena
     *
     * @param restoreInventory true if inventory sould be restored
     * @param players players to restore
     */
    protected void restorePlayers(boolean restoreInventory, Player... players) {
        pm.restorePlayers(restoreInventory, parameterToArray(players));
        //TODO fix removal
        parameterToList(players).forEach(playersInArena::remove);
    }

    protected BattleMode getBattleMode() {
        return hmb.getBattleMode(battleModeType);
    }

    protected List<Player> parameterToList(Player... param) {
        return param != null && param.length > 0 ? Arrays.asList(param) : playersInArena;
    }

    protected Player[] parameterToArray(Player... param) {
        return param != null && param.length > 0 ? param : playersInArena.toArray(new Player[playersInArena.size()]);
    }
}
