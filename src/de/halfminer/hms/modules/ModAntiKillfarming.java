package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;

/**
 * Adds penalty for killing the same player repeatedly
 * - Counts amount of kills
 * - Blocks commands from both players that partook in the killfarming
 * - Doubles punishment
 */
public class ModAntiKillfarming extends HalfminerModule implements Listener {

    /**
     * Time in seconds the player is blocked from teleporting and PvPing
     */
    private static int BLOCK_TIME;
    /**
     * Amount of deaths in short time until block
     */
    private static int THRESHOLD_UNTIL_BLOCK;
    /**
     * Time in seconds until the counter will reset
     */
    private static int THRESHOLD_UNTIL_REMOVAL_SECONDS;

    private final Map<String, String> lang = new HashMap<>();
    private final Set<String> commandExemptList = new HashSet<>();

    private final HashMap<UUID, AntiKillfarmingContainer> deathMap = new HashMap<>();
    private final HashMap<UUID, Long> blockList = new HashMap<>();

    public ModAntiKillfarming() {
        reloadConfig();
    }

    /**
     * Called when a player dies, check if PvP and update anti killfarming variables
     *
     * @param e PlayerDeathEvent
     */
    @EventHandler
    @SuppressWarnings("unused")
    private void onDeathCheckKillfarming(PlayerDeathEvent e) {

        if (e.getEntity().getKiller() != null) {

            Player killer = e.getEntity().getKiller();
            Player victim = e.getEntity().getPlayer();

            if (killer.equals(victim) || killer.hasPermission("hms.bypass.nokillfarming") || victim.hasPermission("hms.bypass.nokillfarming"))
                return;

            //remove the killer from the victims map, to reset the killfarm counter
            if (deathMap.containsKey(victim.getUniqueId()) && deathMap.get(victim.getUniqueId()).containsPlayer(killer)) {
                deathMap.get(victim.getUniqueId()).removePlayer(killer);
            }

            if (deathMap.containsKey(killer.getUniqueId())) {

                AntiKillfarmingContainer container = deathMap.get(killer.getUniqueId());

                if (container.containsPlayer(victim)) { //if the victim is already in the killers list

                    //check if time constraints are met, if they passed the THRESHOLD reset the victim
                    if (container.getTimestamp(victim) + THRESHOLD_UNTIL_REMOVAL_SECONDS > System.currentTimeMillis() / 1000) {

                        container.updatePlayer(victim);
                        //if the kill threshold has already been met, block, else just update
                        if (container.getAmount(victim) == THRESHOLD_UNTIL_BLOCK - 1) { //Warn player
                            killer.sendMessage(Language.placeholderReplace(lang.get("killfarmWarning"), "%PREFIX%", "PvP"));
                            victim.sendMessage(Language.placeholderReplace(lang.get("killfarmWarning"), "%PREFIX%", "PvP"));
                        } else if (container.getAmount(victim) >= THRESHOLD_UNTIL_BLOCK)
                            blockPlayers(killer, victim); //block player

                    } else {
                        container.resetPlayer(victim);
                    }

                } else container.addPlayer(victim); //victim not in killers list, add him

            } else deathMap.put(killer.getUniqueId(), new AntiKillfarmingContainer(victim));
        }

    }

    /**
     * Check if players are engaging in PvP, prevent if blocked and send messages
     *
     * @param e EntityDamageByEntityEvent that was fired
     */
    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onPvPCheckIfAllowed(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            short time = getBlockTime((Player) e.getDamager());
            if (time >= 0) {
                e.getDamager().sendMessage(Language.placeholderReplace(lang.get("noPvPAttack"), "%TIME%", Short.toString(time)));
                e.setCancelled(true);
                return;
            }
            time = getBlockTime((Player) e.getEntity());
            if (time >= 0) {
                e.getDamager().sendMessage(Language.placeholderReplace(lang.get("noPvPProtect"), "%PLAYER%", e.getEntity().getName(), "%TIME%", Short.toString(time)));
                e.setCancelled(true);
                return;
            }
        }

        //prevent death by tnt ignition
        if (e.getDamager() instanceof TNTPrimed && e.getEntity() instanceof Player) {
            TNTPrimed tnt = (TNTPrimed) e.getDamager();
            if (tnt.getSource() instanceof Player) {
                Player victim = (Player) e.getEntity();
                short time = getBlockTime(victim);
                if (time >= 0) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        if (e.getEntity() instanceof Player && e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player) {
            Player attacker = (Player) ((Projectile) e.getDamager()).getShooter();
            Player victim = (Player) e.getEntity();

            if (attacker.equals(victim)) { //Disallow hitting yourself with bow
                e.setCancelled(true);
                return;
            }

            short timeAttacker = getBlockTime(attacker);
            short timeVictim = getBlockTime(victim);
            if (timeAttacker >= 0) {
                attacker.sendMessage(Language.placeholderReplace(lang.get("noPvPAttack"), "%TIME%", Short.toString(timeAttacker)));
                e.setCancelled(true);
                return;
            }
            if (timeVictim >= 0) {
                attacker.sendMessage(Language.placeholderReplace(lang.get("noPvPProtect"), "%PLAYER%", e.getEntity().getName(), "%TIME%", Short.toString(timeVictim)));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onCommandCheckIfAllowed(PlayerCommandPreprocessEvent e) {
        short time = getBlockTime(e.getPlayer());
        if (time > 0) {
            String command = e.getMessage().split(" ")[0].toLowerCase();
            if (!commandExemptList.contains(command.substring(1, command.length()))) {
                e.getPlayer().sendMessage(Language.placeholderReplace(lang.get("noCommand"), "%TIME%", Short.toString(time)));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onPotionCheckIfAllowed(PotionSplashEvent e) {
        if (e.getEntity().getShooter() instanceof Player) {
            Player thrower = (Player) e.getEntity().getShooter();
            short time = getBlockTime(thrower);
            if (time >= 0) {
                thrower.sendMessage(Language.placeholderReplace(lang.get("noPvPAttack"), "%TIME%", Short.toString(time)));
                e.setCancelled(true);
            }
        }
    }

    /**
     * Get time that is left for the block
     *
     * @param player - player to check
     * @return -1 if not blocked, else short in seconds
     */
    private short getBlockTime(Player player) {
        if (blockList.containsKey(player.getUniqueId())) {

            long currentTime = System.currentTimeMillis() / 1000;
            long blockTime = blockList.get(player.getUniqueId());

            if (blockTime - currentTime > 0) return (short) (blockTime - currentTime);
            else {
                blockList.remove(player.getUniqueId()); //time is up, remove and return that the player is not blocked
                return -1;
            }
        } else return -1; //not blocked
    }

    /**
     * Block two players who killfarmed
     *
     * @param killer the killfarmer
     * @param victim the player, who got farmed
     */
    private void blockPlayers(Player killer, Player victim) {

        //Get containers
        AntiKillfarmingContainer killerVal = deathMap.get(killer.getUniqueId());
        if (!deathMap.containsKey(victim.getUniqueId()))
            deathMap.put(victim.getUniqueId(), new AntiKillfarmingContainer());
        AntiKillfarmingContainer victimVal = deathMap.get(victim.getUniqueId());

        killerVal.incrementAmountBlocked();
        victimVal.incrementAmountBlocked();

        //Determine block time, since it doubles every time you get blocked
        long blockTimeKiller = BLOCK_TIME;
        long blockTimeVictim = BLOCK_TIME;
        long systemTime = (System.currentTimeMillis() / 1000);

        for (int i = 1; i < killerVal.getAmountBlocked(); i++) blockTimeKiller *= 2;
        for (int i = 1; i < victimVal.getAmountBlocked(); i++) blockTimeVictim *= 2;

        //update the player in the killers kill list, ensuring that once one of the blocks (either killer or victim)
        //runs out, and the killer kills the victim again, it will still block them both
        killerVal.updatePlayer(victim, blockTimeKiller > blockTimeVictim ? blockTimeKiller : blockTimeVictim);

        //add to block list
        blockList.put(killer.getUniqueId(), systemTime + blockTimeKiller);
        blockList.put(victim.getUniqueId(), systemTime + blockTimeVictim);

        //send messages
        hms.getServer().broadcastMessage(Language.placeholderReplace(lang.get("blockedBroadcast"), "%KILLER%", killer.getName(), "%VICTIM%", victim.getName()));
        killer.sendMessage(Language.placeholderReplace(lang.get("blockedKiller"), "%TIME%", Long.toString(blockTimeKiller / 60), "%PLAYER%", victim.getName()));
        victim.sendMessage(Language.placeholderReplace(lang.get("blockedVictim"), "%TIME%", Long.toString(blockTimeVictim / 60), "%PLAYER%", killer.getName()));
    }

    @Override
    public void reloadConfig() {

        //Get constants
        BLOCK_TIME = hms.getConfig().getInt("killfarming.blockTime", 300);
        THRESHOLD_UNTIL_BLOCK = hms.getConfig().getInt("killfarming.thresholdUntilBlock", 5);
        THRESHOLD_UNTIL_REMOVAL_SECONDS = hms.getConfig().getInt("killfarming.thresholdUntilRemoval", 100);

        //Get allowed commands
        commandExemptList.clear();
        commandExemptList.addAll(hms.getConfig().getStringList("killfarming.killfarmingCommandExemptions"));

        //Get language
        lang.clear();
        lang.put("killfarmWarning", Language.getMessagePlaceholders("modAntiKillfarmingWarning", true, "%PREFIX%", "PvP"));
        lang.put("noCommand", Language.getMessagePlaceholders("modAntiKillfarmingNoCommand", true, "%PREFIX%", "Info"));
        lang.put("noPvPAttack", Language.getMessagePlaceholders("modAntiKillfarmingNoPvPAttack", true, "%PREFIX%", "PvP"));
        lang.put("noPvPProtect", Language.getMessagePlaceholders("modAntiKillfarmingNoPvPProtect", true, "%PREFIX%", "PvP"));
        lang.put("blockedBroadcast", Language.getMessagePlaceholders("modAntiKillfarmingBlockedBroadcast", true, "%PREFIX%", "PvP"));
        lang.put("blockedKiller", Language.getMessagePlaceholders("modAntiKillfarmingBlockedKiller", true, "%PREFIX%", "PvP"));
        lang.put("blockedVictim", Language.getMessagePlaceholders("modAntiKillfarmingBlockedVictim", true, "%PREFIX%", "PvP"));
    }

    /**
     * Class containing the information about which players
     * were killed, how often, when, and how many blocks a player already had
     */
    private class AntiKillfarmingContainer {

        /**
         * Map containing the players that got killed by the containers owner
         */
        final HashMap<UUID, Values> players = new HashMap<>();
        /**
         * Total amount player got blocked already, the higher it is, the longer the block
         */
        byte amountBlocked = 0;

        AntiKillfarmingContainer() {
        }

        AntiKillfarmingContainer(Player victim) {
            players.put(victim.getUniqueId(), new Values());
        }

        boolean containsPlayer(Player toCheck) {
            return players.containsKey(toCheck.getUniqueId());
        }

        void addPlayer(Player toAdd) {
            players.put(toAdd.getUniqueId(), new Values());
        }

        void removePlayer(Player toRemove) {
            players.remove(toRemove.getUniqueId());
        }

        /**
         * Update a players last killed timestamp and amount he died
         *
         * @param toUpdate - Player that will be updated
         */
        void updatePlayer(Player toUpdate) {
            updatePlayer(toUpdate, 0);
        }

        /**
         * Update a players timestamp and amount
         *
         * @param toUpdate - Player that will be updated
         * @param timeDiff - additional time that will be added to the threshold,
         *                 useful for making sure that after a block the players can't proceed with killfarming
         */
        void updatePlayer(Player toUpdate, long timeDiff) {
            Values val = players.get(toUpdate.getUniqueId());
            val.amount++;
            val.timestamp = (System.currentTimeMillis() / 1000) + timeDiff;
        }

        /**
         * Reset a players killcount back to 1
         *
         * @param toReset player who will be reset
         */
        void resetPlayer(Player toReset) {
            Values val = players.get(toReset.getUniqueId());
            val.amount = 1;
            val.timestamp = System.currentTimeMillis() / 1000;
        }

        void incrementAmountBlocked() {
            this.amountBlocked++;
        }

        /**
         * Get the time until the player that died should be reset
         *
         * @param toGet player whose last killed timestamp to get
         * @return the timestamp, when passed the player should be reset
         */
        long getTimestamp(Player toGet) {
            return players.get(toGet.getUniqueId()).timestamp;
        }

        short getAmount(Player toGet) {
            return players.get(toGet.getUniqueId()).amount;
        }

        byte getAmountBlocked() {
            return amountBlocked;
        }

        /**
         * Wrapper for death amount of the containers victim and when the last kill happened
         */
        class Values {
            long timestamp = System.currentTimeMillis() / 1000;
            short amount = 1;
        }

    }

}
