package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Pair;
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
 * - Counts amount of kills
 * - Blocks commands from both players that partook in the killfarming
 * - Doubles punishment
 */
@SuppressWarnings("unused")
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

    private final Map<UUID, AntiKillfarmingContainer> deathMap = new HashMap<>();
    private final Map<UUID, Long> blockList = new HashMap<>();

    /**
     * Called when a player dies, check if PvP and update anti killfarming variables
     *
     * @param e PlayerDeathEvent
     */
    @EventHandler
    private void onDeathCheckKillfarming(PlayerDeathEvent e) {

        if (e.getEntity().getKiller() != null) {

            Player killer = e.getEntity().getKiller();
            Player victim = e.getEntity().getPlayer();

            if (killer.equals(victim) || killer.hasPermission("hms.bypass.nokillfarming")
                    || victim.hasPermission("hms.bypass.nokillfarming")) return;

            // remove the killer from the victims map, to reset the killfarm counter
            if (deathMap.containsKey(victim.getUniqueId())
                    && deathMap.get(victim.getUniqueId()).containsPlayer(killer)) {
                deathMap.get(victim.getUniqueId()).removePlayer(killer);
            }

            if (deathMap.containsKey(killer.getUniqueId())) {

                AntiKillfarmingContainer container = deathMap.get(killer.getUniqueId());

                // if the victim is already in the killers list
                if (container.containsPlayer(victim)) {

                    // check if time constraints are met, if they passed the THRESHOLD reset the victim
                    if (container.getTimestamp(victim) + THRESHOLD_UNTIL_REMOVAL_SECONDS
                            > System.currentTimeMillis() / 1000) {

                        container.updatePlayer(victim, 0);
                        // if the kill threshold has already been met, block, else just update
                        if (container.getAmount(victim) == THRESHOLD_UNTIL_BLOCK - 1) { //Warn player
                            killer.sendMessage(Language.placeholderReplace(lang.get("killfarmWarning"),
                                    "%PREFIX%", "PvP"));
                            victim.sendMessage(Language.placeholderReplace(lang.get("killfarmWarning"),
                                    "%PREFIX%", "PvP"));
                        } else if (container.getAmount(victim) >= THRESHOLD_UNTIL_BLOCK)
                            blockPlayers(killer, victim);

                    } else {
                        container.resetPlayer(victim);
                    }

                } else container.addPlayer(victim);

            } else deathMap.put(killer.getUniqueId(), new AntiKillfarmingContainer(victim));
        }
    }

    /**
     * Check if players are engaging in PvP, prevent if blocked and send messages
     *
     * @param e EntityDamageByEntityEvent that was fired
     */
    @EventHandler(ignoreCancelled = true)
    public void onPvPCheckIfAllowed(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player && e.getDamager() instanceof Player) {
            int time = getBlockTime((Player) e.getDamager());
            if (time >= 0) {
                e.getDamager().sendMessage(Language.placeholderReplace(lang.get("noPvPAttack"),
                        "%TIME%", String.valueOf(time)));
                e.setCancelled(true);
                return;
            }
            time = getBlockTime((Player) e.getEntity());
            if (time >= 0) {
                e.getDamager().sendMessage(Language.placeholderReplace(lang.get("noPvPProtect"),
                        "%PLAYER%", e.getEntity().getName(), "%TIME%", String.valueOf(time)));
                e.setCancelled(true);
                return;
            }
        }

        // prevent death by tnt ignition
        if (e.getDamager() instanceof TNTPrimed && e.getEntity() instanceof Player) {
            TNTPrimed tnt = (TNTPrimed) e.getDamager();
            if (tnt.getSource() instanceof Player) {
                Player victim = (Player) e.getEntity();
                int time = getBlockTime(victim);
                if (time >= 0) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        if (e.getEntity() instanceof Player
                && e.getDamager() instanceof Projectile
                && ((Projectile) e.getDamager()).getShooter() instanceof Player) {
            Player attacker = (Player) ((Projectile) e.getDamager()).getShooter();
            Player victim = (Player) e.getEntity();

            // disallow hitting self with bow
            if (attacker.equals(victim)) {
                e.setCancelled(true);
                return;
            }

            int timeAttacker = getBlockTime(attacker);
            int timeVictim = getBlockTime(victim);
            if (timeAttacker >= 0) {
                attacker.sendMessage(Language.placeholderReplace(lang.get("noPvPAttack"),
                        "%TIME%", String.valueOf(timeAttacker)));
                e.setCancelled(true);
                return;
            }
            if (timeVictim >= 0) {
                attacker.sendMessage(Language.placeholderReplace(lang.get("noPvPProtect"),
                        "%PLAYER%", e.getEntity().getName(), "%TIME%", String.valueOf(timeVictim)));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommandCheckIfAllowed(PlayerCommandPreprocessEvent e) {

        int time = getBlockTime(e.getPlayer());
        if (time > 0) {
            String command = e.getMessage().split(" ")[0].toLowerCase();
            if (!commandExemptList.contains(command.substring(1, command.length()))) {
                e.getPlayer().sendMessage(Language.placeholderReplace(lang.get("noCommand"),
                        "%TIME%", String.valueOf(time)));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionCheckIfAllowed(PotionSplashEvent e) {

        if (e.getEntity().getShooter() instanceof Player) {
            Player thrower = (Player) e.getEntity().getShooter();
            int time = getBlockTime(thrower);
            if (time >= 0) {
                thrower.sendMessage(Language.placeholderReplace(lang.get("noPvPAttack"),
                        "%TIME%", String.valueOf(time)));
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
    private int getBlockTime(Player player) {

        if (blockList.containsKey(player.getUniqueId())) {

            long currentTime = System.currentTimeMillis() / 1000;
            long blockTime = blockList.get(player.getUniqueId());

            if (blockTime - currentTime > 0) return (short) (blockTime - currentTime);
            else {
                // time is up, remove and return that the player is not blocked
                blockList.remove(player.getUniqueId());
                return -1;
            }
        } else return -1;
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

        // Determine block time, since it doubles every time you get blocked
        long blockTimeKiller = BLOCK_TIME;
        long blockTimeVictim = BLOCK_TIME;
        long systemTime = (System.currentTimeMillis() / 1000);

        for (int i = 1; i < killerVal.getAmountBlocked(); i++) blockTimeKiller *= 2;
        for (int i = 1; i < victimVal.getAmountBlocked(); i++) blockTimeVictim *= 2;

        /* update the player in the killers kill list, ensuring that once one of the blocks (either killer or victim)
        runs out, and the killer kills the victim again, it will still block them both */
        killerVal.updatePlayer(victim, blockTimeKiller > blockTimeVictim ? blockTimeKiller : blockTimeVictim);

        // add to block list
        blockList.put(killer.getUniqueId(), systemTime + blockTimeKiller);
        blockList.put(victim.getUniqueId(), systemTime + blockTimeVictim);

        // send messages
        server.broadcastMessage(Language.placeholderReplace(lang.get("blockedBroadcast"),
                "%KILLER%", killer.getName(), "%VICTIM%", victim.getName()));
        killer.sendMessage(Language.placeholderReplace(lang.get("blockedKiller"),
                "%TIME%", Long.toString(blockTimeKiller / 60), "%PLAYER%", victim.getName()));
        victim.sendMessage(Language.placeholderReplace(lang.get("blockedVictim"),
                "%TIME%", Long.toString(blockTimeVictim / 60), "%PLAYER%", killer.getName()));
    }

    @Override
    public void loadConfig() {

        // Get constants
        BLOCK_TIME = config.getInt("antiKillfarming.blockTime", 300);
        THRESHOLD_UNTIL_BLOCK = config.getInt("antiKillfarming.thresholdUntilBlock", 5);
        THRESHOLD_UNTIL_REMOVAL_SECONDS = config.getInt("antiKillfarming.thresholdUntilRemoval", 100);

        // Get allowed commands
        commandExemptList.clear();
        commandExemptList.addAll(config.getStringList("antiKillfarming.killfarmingCommandExemptions"));

        // Get language
        lang.clear();
        lang.put("killfarmWarning", Language.getMessagePlaceholders("modAntiKillfarmingWarning", true,
                "%PREFIX%", "PvP"));
        lang.put("noCommand", Language.getMessagePlaceholders("modAntiKillfarmingNoCommand",
                true, "%PREFIX%", "Info"));
        lang.put("noPvPAttack", Language.getMessagePlaceholders("modAntiKillfarmingNoPvPAttack",
                true, "%PREFIX%", "PvP"));
        lang.put("noPvPProtect", Language.getMessagePlaceholders("modAntiKillfarmingNoPvPProtect",
                true, "%PREFIX%", "PvP"));
        lang.put("blockedBroadcast", Language.getMessagePlaceholders("modAntiKillfarmingBlockedBroadcast",
                true, "%PREFIX%", "PvP"));
        lang.put("blockedKiller", Language.getMessagePlaceholders("modAntiKillfarmingBlockedKiller",
                true, "%PREFIX%", "PvP"));
        lang.put("blockedVictim", Language.getMessagePlaceholders("modAntiKillfarmingBlockedVictim",
                true, "%PREFIX%", "PvP"));
    }

    /**
     * Class containing the information about which players
     * were killed, how often, when, and how many blocks a player already had
     */
    private class AntiKillfarmingContainer {

        /**
         * Map containing the players that got killed by the containers owner
         * Pair mapping: Left - Timestamp, Right - Counter (amount of kills)
         */
        final HashMap<UUID, Pair<Long, Integer>> players = new HashMap<>();

        /**
         * Total amount player got blocked already, the higher it is, the longer the block
         */
        int amountBlocked = 0;

        AntiKillfarmingContainer() {
        }

        AntiKillfarmingContainer(Player victim) {
            players.put(victim.getUniqueId(), getDefaultPair());
        }

        boolean containsPlayer(Player toCheck) {
            return players.containsKey(toCheck.getUniqueId());
        }

        void addPlayer(Player toAdd) {
            players.put(toAdd.getUniqueId(), getDefaultPair());
        }

        void removePlayer(Player toRemove) {
            players.remove(toRemove.getUniqueId());
        }

        /**
         * Update a players timestamp and amount
         *
         * @param toUpdate - Player that will be updated
         * @param timeDiff - additional time that will be added to the threshold,
         *                 useful for making sure that after a block the players can't proceed with killfarming
         */
        void updatePlayer(Player toUpdate, long timeDiff) {

            Pair<Long, Integer> pair = players.get(toUpdate.getUniqueId());
            pair.setLeft((System.currentTimeMillis() / 1000) + timeDiff);
            pair.setRight(pair.getRight() + 1);
        }

        /**
         * Reset a players killcount back to 1
         *
         * @param toReset player who will be reset
         */
        void resetPlayer(Player toReset) {
            players.put(toReset.getUniqueId(), getDefaultPair());
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
            return players.get(toGet.getUniqueId()).getLeft();
        }

        int getAmount(Player toGet) {
            return players.get(toGet.getUniqueId()).getRight();
        }

        int getAmountBlocked() {
            return amountBlocked;
        }

        private Pair<Long, Integer> getDefaultPair() {
            return new Pair<>(System.currentTimeMillis() / 1000, 1);
        }
    }
}
