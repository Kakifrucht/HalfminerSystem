package de.halfminer.hmduel.module;

import de.halfminer.hmduel.HalfminerDuel;
import de.halfminer.hmduel.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.potion.PotionEffect;

import java.util.Arrays;

class Arena {

    private static final HalfminerDuel hmd = HalfminerDuel.getInstance();

    private boolean isFree = true;

    //Arena information
    private final String name;
    private Location locationA;
    private Location locationB;
    private ItemStack[] kitArmor;
    private ItemStack[] kitContent;

    //Player information
    private Player playerA;
    private Player playerB;
    private PlayerDataContainer playerAContainer;
    private PlayerDataContainer playerBContainer;

    //Scheduler
    private int taskId;
    private int timeLeft;

    public Arena(String name, Location[] locations, ItemStack[] kit) {

        this.name = name;
        this.locationA = locations[0];
        this.locationB = locations[1];
        setKit(kit);

    }

    /**
     * @return true if arena is currently free, false if in use
     */
    public boolean isFree() {
        return isFree;
    }

    /**
     * @return The arenas name
     */
    public String getName() {
        return name;
    }

    /**
     * @return PlayerA in the arena, null if empty
     */
    public Player getPlayerA() {
        return playerA;
    }

    /**
     * @return PlayerB in the arena, null if empty
     */
    public Player getPlayerB() {
        return playerB;
    }

    /**
     * Returns the arenas locations, used when arenas are reloaded to update them
     * @return Location array, where index 0 is spawnA and index 1 spawnB
     */
    public Location[] getLocations() {
        Location[] toReturn = new Location[2];
        toReturn[0] = locationA;
        toReturn[1] = locationB;
        return toReturn;
    }

    /**
     * Set the location of the arena
     * @param loc to set the spawn from
     * @param locationA true if spawnA is being set, false if spawnB
     */
    public void setLocation(Location loc, boolean locationA) {
        if(locationA) this.locationA = loc;
        else this.locationB = loc;
    }

    /**
     * (Re)set the arenas kit
     * @param kit array containing the kit, where index 0-3 are armor and 4-39 is content
     */
    public void setKit(ItemStack[] kit) {
        this.kitArmor = Arrays.copyOfRange(kit, 0, 4);
        this.kitContent = Arrays.copyOfRange(kit, 4, 40);
    }

    /**
     * Issues the arena to lock and start the game, prepares the fight
     * @param a playerA in the arena
     * @param b playerB in the arena
     */
    public void gameStart(Player a, Player b) {

        isFree = false;

        this.playerA = a;
        this.playerB = b;

        beforeFight(a);
        beforeFight(b);

        Util.sendMessage(a, "gameStartingCountdown", new String[]{"%PLAYER%", b.getName(), "%ARENA%", name});
        Util.sendMessage(b, "gameStartingCountdown", new String[]{"%PLAYER%", a.getName(), "%ARENA%", name});

        if(hmd.getConfig().getInt("gameTime") > 20) timeLeft = hmd.getConfig().getInt("gameTime") + 5;
        else timeLeft = 305; //set default if config value is too low
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(hmd, new Runnable() {

            @Override
            public void run() {

                timeLeft -= 5;
                if (timeLeft == hmd.getConfig().getInt("gameTime")) { //Battle ist starting, reset walkspeed and give the kit
                    playerA.setWalkSpeed(0.2F);
                    playerB.setWalkSpeed(0.2F);
                    Util.sendMessage(playerA, "gameStarting");
                    Util.sendMessage(playerB, "gameStarting");
                    playerA.getInventory().setContents(kitContent);
                    playerA.getInventory().setArmorContents(kitArmor);
                    playerB.getInventory().setContents(kitContent);
                    playerB.getInventory().setArmorContents(kitArmor);
                }
                if (timeLeft <= 15 && timeLeft > 0) {
                    Util.sendMessage(playerA, "gameTimeRunningOut", new String[]{"%TIME%", Integer.toString(timeLeft)});
                    Util.sendMessage(playerB, "gameTimeRunningOut", new String[]{"%TIME%", Integer.toString(timeLeft)});
                }
                if (timeLeft <= 0) {
                    HalfminerDuel.getInstance().getArenaQueue().gameHasFinished(playerA, false);
                    return;
                }
                if (timeLeft <= -1) { //just to safeguard from having alot of tasks that do not cancel and throw exceptions
                    Bukkit.getScheduler().cancelTask(taskId);
                }

            }

        }, 100L, 100L);
    }

    /**
     * Called once a game ends. Will issue restoring of players and resetting.
     * This happens due to death of a player (duel win), time running out, reloading of plugin
     * or logging out.
     * @param loser player that caused the game end
     * @param hasWinner true if a duel has a winner (logout or death), false if not (reload, time ran out)
     */
    public void gameEnd(Player loser, boolean hasWinner) {

        Bukkit.getScheduler().cancelTask(taskId);

        if(playerA.isDead()) afterFightDead(playerA);
        else {
            healPlayer(playerA);
            restorePlayer(playerA);
        }

        if(playerB.isDead()) afterFightDead(playerB);
        else {
            healPlayer(playerB);
            restorePlayer(playerB);
        }

        if(hasWinner) {
            Player winner = loser.equals(playerA) ? playerB : playerA;
            Util.sendMessage(winner, "gameWon", new String[]{"%PLAYER%", loser.getName()});
            Util.sendMessage(loser, "gameLost", new String[]{"%PLAYER%", winner.getName()});
            if (hmd.getConfig().getBoolean("broadcastWin"))
                Util.broadcastMessage("gameBroadcast", new String[]{"%WINNER%", winner.getName(), "%LOSER%", loser.getName(), "%ARENA%", name}, Arrays.asList(winner, loser));
        } else {
            Util.sendMessage(playerA, "gameTied", new String[]{"%PLAYER%", playerB.getName()});
            Util.sendMessage(playerB, "gameTied", new String[]{"%PLAYER%", playerA.getName()});
        }

        isFree = true;

    }

    /**
     * Heals player before fight, stores his data, tps him into arena
     * @param player player to heal/store/tp
     */
    private void beforeFight(Player player) {
        //Store data about user
        PlayerDataContainer data = new PlayerDataContainer();
        data.loc = player.getLocation();
        data.inventory = player.getInventory().getContents();
        data.armor = player.getInventory().getArmorContents();

        //Make sure that invs are closed and no mounts are taken into arena
        if(player.isInsideVehicle()) player.leaveVehicle();
        player.closeInventory();

        //Store and teleport
        boolean isPlayerA = player.equals(playerA);
        if(isPlayerA) {
            playerAContainer = data;
            player.teleport(locationA);
        } else {
            playerBContainer = data;
            player.teleport(locationB);
        }

        healPlayer(player);

        //Clear inventory
        PlayerInventory inv = player.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        player.updateInventory();

        //5 seconds freeze
        player.setWalkSpeed(0.0F);
    }

    /**
     * Heals and restores player, however also respawns him and does it one tick later to not cause issues
     * @param player player to respawn, heal and restore
     */
    private void afterFightDead(final Player player) {

        try { //player is dead, restore him one tick later
            Bukkit.getScheduler().scheduleSyncDelayedTask(hmd, new Runnable() {
                @Override
                public void run() {
                    player.spigot().respawn();
                    healPlayer(player);
                    restorePlayer(player);
                }
            });
        } catch (IllegalPluginAccessException e) { //only happens during reload (onDisable call) and if a player died in the same moment
            hmd.getLogger().info(player.getName() + " duel was cancelled while the player was dead already");
            restorePlayer(player); //still restore
        }

    }

    /**
     * Heals a player
     * @param player will be healed
     */
    private void healPlayer(final Player player) {

        player.setHealth(player.getHealthScale());
        player.setFoodLevel(20);
        player.setSaturation(10);
        player.setExhaustion(0F);
        player.setFireTicks(0);
        for(PotionEffect effect: player.getActivePotionEffects()) player.removePotionEffect(effect.getType());

    }

    /**
     * Restore the players state pre duel
     * @param player to restore the state from
     */
    private void restorePlayer(Player player) {

        PlayerDataContainer data = player.equals(playerA) ? playerAContainer : playerBContainer;
        player.teleport(data.loc);
        player.closeInventory();
        player.getInventory().setContents(data.inventory);
        player.getInventory().setArmorContents(data.armor);
        player.updateInventory();
        player.setWalkSpeed(0.2F);

    }

    /**
     * Containing stored data about player before the duel to restore it after the fight
     */
    private class PlayerDataContainer {

        public Location loc;
        public ItemStack[] inventory;
        public ItemStack[] armor;

    }

}
