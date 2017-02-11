package de.halfminer.hmb.arena.abs;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.GlobalMode;
import de.halfminer.hmb.mode.abs.GameMode;
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
    public boolean isFree() {
        return !spawns.isEmpty();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public GameModeType getGameModeType() {
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

        GlobalMode global = (GlobalMode) hmb.getGameMode(GameModeType.GLOBAL);
        for (Location spawn : spawns) {
            if (spawn.getWorld().equals(loc.getWorld()) && spawn.distance(loc) <= global.getTeleportSpawnDistance())
                return true;
        }

        return false;
    }

    /**
     * Add the specified players to the arena
     *
     * @param players to be added to the arena
     */
    public void addPlayers(Player... players) {
        if (isFree()) {
            Collections.addAll(playersInArena, players);
        } else throw new RuntimeException("Tried to add players to an occupied arena");
    }

    protected void teleportIntoArena(Player... toTeleport) {

        int spawnNumber = 0;
        for (Player player : parameterToList(toTeleport)) {
            if (!player.teleport(spawns.get(Math.min(spawnNumber++, spawns.size() - 1)))) {
                hmb.getLogger().warning("Player " + player.getName() + " could not be teleported into the arena");
            }
        }
    }

    protected void storeAndTeleportPlayers(Player... players) {
        pm.setArena(this, parameterToArray(players));
        teleportIntoArena(players);
    }

    protected void restorePlayers(boolean restoreInventory, Player... players) {
        pm.restorePlayers(restoreInventory, parameterToArray(players));
    }

    protected void healAndPreparePlayers(Player... players) {
        for (Player toHeal : parameterToList(players)) {
            toHeal.setHealth(20.0d);
            toHeal.setFoodLevel(20);
            toHeal.setSaturation(10);
            toHeal.setExhaustion(0F);
            toHeal.setFireTicks(0);
            for (PotionEffect effect : toHeal.getActivePotionEffects())
                toHeal.removePotionEffect(effect.getType());
            toHeal.setGameMode(org.bukkit.GameMode.ADVENTURE);
        }
    }

    protected GameMode getGameMode() {
        return hmb.getGameMode(gameMode);
    }

    protected List<Player> parameterToList(Player... param) {
        return param != null && param.length > 0 ? Arrays.asList(param) : playersInArena;
    }

    protected Player[] parameterToArray(Player... param) {
        return param != null && param.length > 0 ? param : playersInArena.toArray(new Player[playersInArena.size()]);
    }
}
