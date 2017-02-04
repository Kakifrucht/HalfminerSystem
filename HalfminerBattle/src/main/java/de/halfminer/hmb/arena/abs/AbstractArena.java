package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.GlobalMode;
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
    protected final GameModeType gameMode;
    protected final String name;
    protected List<Location> spawns = new ArrayList<>();
    protected final LinkedList<Player> playersInArena = new LinkedList<>();

    protected AbstractArena(GameModeType gameMode, String name) {
        this.gameMode = gameMode;
        this.name = name;
    }

    @Override
    public boolean isActive() {
        return !spawns.isEmpty();
    }

    @Override
    public boolean isFree() {
        return isActive();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isCloseToSpawn(Location loc) {

        GlobalMode global = (GlobalMode) hmb.getGameMode(GameModeType.GLOBAL);
        for (Location spawn : spawns) {
            if (spawn.distance(loc) <= global.getTeleportSpawnDistance()) return true;
        }

        return false;
    }

    @Override
    public GameModeType getGameMode() {
        return gameMode;
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
    public List<Location> getSpawns() {
        return spawns;
    }

    @Override
    public void addPlayers(Player... players) {
        Collections.addAll(playersInArena, players);
    }

    protected void healPlayers(Player... players) {
        for (Player toHeal : parameterToList(players)) {
            toHeal.setHealth(20.0d);
            toHeal.setFoodLevel(20);
            toHeal.setSaturation(10);
            toHeal.setExhaustion(0F);
            toHeal.setFireTicks(0);
            for (PotionEffect effect : toHeal.getActivePotionEffects())
                toHeal.removePotionEffect(effect.getType());
        }
    }

    protected List<Player> parameterToList(Player... param) {
        return param != null && param.length > 0 ? Arrays.asList(param) : playersInArena;
    }
}
