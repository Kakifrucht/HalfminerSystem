package de.halfminer.hms.modules;

import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.Pair;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.*;

/**
 * - Counts amount of kills between two players
 * - After set amount of kills has been passed, blocks players for a set amount of time
 *   - Checks time between kills, resets if kills passed time
 *   - Broadcasts block to all players
 *     - Warns players one kill before block
 *     - Also prints informational message that it is allowed
 *   - Blocks further PvP
 *     - Hitting
 *     - TnT killing
 *     - Arrow shooting
 *     - Splash potion throwing
 *   - Blocks commands
 *   - Prints message with remaining block time
 * - Punishment will double if players do not stop killfarming immediately
 * - Doubles punishment
 */
@SuppressWarnings("unused")
public class ModAntiKillfarming extends HalfminerModule implements Listener, Sweepable {

    private int blockTime;
    private int thresholdUntilBlock;
    private int thresholdUntilRemovalSeconds;

    private final Map<String, String> lang = new HashMap<>();
    private final Set<String> exemptCommands = new HashSet<>();

    private final Map<UUID, AntiKillfarmingContainer> containerMap = new HashMap<>();

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

            if (killer.equals(victim)
                    || killer.hasPermission("hms.bypass.nokillfarming")
                    || victim.hasPermission("hms.bypass.nokillfarming"))
                return;

            // remove the killer from the victims map, to reset the killfarm counter
            if (containerMap.containsKey(victim.getUniqueId())) {
                containerMap.get(victim.getUniqueId()).removePlayer(killer);
            }

            AntiKillfarmingContainer container;
            if (!containerMap.containsKey(killer.getUniqueId())) {

                container = new AntiKillfarmingContainer();
                containerMap.put(killer.getUniqueId(), container);
            } else container = containerMap.get(killer.getUniqueId());

            container.incrementPlayer(victim, 0);

            if (container.getAmountKilled(victim) == thresholdUntilBlock - 1) {
                // warn players
                killer.sendMessage(Language.placeholderReplace(lang.get("killfarmWarning"), "%PREFIX%", "PvP"));
                victim.sendMessage(Language.placeholderReplace(lang.get("killfarmWarning"), "%PREFIX%", "PvP"));
            } else if (container.getAmountKilled(victim) >= thresholdUntilBlock) blockPlayers(killer, victim);
        }
    }

    /**
     * Check if players are engaging in PvP, prevent if blocked and send messages
     *
     * @param e EntityDamageByEntityEvent that was fired
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvPCheckIfAllowed(EntityDamageByEntityEvent e) {

        // direct PvP check
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

        // arrow hit check
        if (e.getEntity() instanceof Player
                && e.getDamager() instanceof Projectile
                && ((Projectile) e.getDamager()).getShooter() instanceof Player) {

            Player attacker = (Player) ((Projectile) e.getDamager()).getShooter();
            Player victim = (Player) e.getEntity();

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
    public void onCombustCheckIfAllowed(EntityCombustByEntityEvent e) {

        if (e.getEntity() instanceof Player
                && e.getCombuster() instanceof Projectile
                && ((Projectile) e.getCombuster()).getShooter() instanceof Player) {

            if (getBlockTime((Player) ((Arrow) e.getCombuster()).getShooter()) > 0
                    || getBlockTime((Player) e.getEntity()) > 0)
                e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommandCheckIfAllowed(PlayerCommandPreprocessEvent e) {

        int time = getBlockTime(e.getPlayer());
        if (time > 0) {
            String command = e.getMessage().split(" ")[0].toLowerCase();
            if (!exemptCommands.contains(command.substring(1, command.length()))) {
                e.getPlayer().sendMessage(Language.placeholderReplace(lang.get("noCommand"),
                        "%TIME%", String.valueOf(time)));
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionCheckIfAllowed(PotionSplashEvent e) {
        e.setCancelled(splashPotionCheck(e));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLingeringPotionCheckIfAllowed(LingeringPotionSplashEvent e) {
        e.setCancelled(splashPotionCheck(e));
    }

    private boolean splashPotionCheck(ProjectileHitEvent e) {

        if (e.getEntity().getShooter() instanceof Player) {
            Player thrower = (Player) e.getEntity().getShooter();
            int time = getBlockTime(thrower);
            if (time >= 0) {
                thrower.sendMessage(Language.placeholderReplace(lang.get("noPvPAttack"),
                        "%TIME%", String.valueOf(time)));
                return true;
            }
        }
        return false;
    }

    /**
     * Get time that is left for the block
     *
     * @param player - player to check
     * @return -1 if not blocked, else short in seconds
     */
    private int getBlockTime(Player player) {

        if (containerMap.containsKey(player.getUniqueId())) {

            return (int) containerMap.get(player.getUniqueId()).getBlockTime();
        } else return -1;
    }

    /**
     * Block two players who killfarmed
     *
     * @param killer the killfarmer
     * @param victim the player, who got farmed
     */
    private void blockPlayers(Player killer, Player victim) {

        // Get containers
        AntiKillfarmingContainer killerVal = containerMap.get(killer.getUniqueId());
        AntiKillfarmingContainer victimVal;
        if (!containerMap.containsKey(victim.getUniqueId())) {
            victimVal = new AntiKillfarmingContainer();
            containerMap.put(victim.getUniqueId(), victimVal);
        } else victimVal = containerMap.get(victim.getUniqueId());

        // Determine block time, since it doubles every time you get blocked
        long blockTimeKiller, blockTimeVictim;
        blockTimeKiller = blockTimeVictim = blockTime;

        for (int i = 1; i < killerVal.amountBlocked + 1; i++) blockTimeKiller *= 2;
        for (int i = 1; i < victimVal.amountBlocked + 1; i++) blockTimeVictim *= 2;

        /*
           Update the player in the killers kill list, ensuring that once both blocks
           run out, and the killer kills the victim again, they will both be blocked again
        */
        killerVal.incrementPlayer(victim, blockTimeKiller > blockTimeVictim ? blockTimeKiller : blockTimeVictim);

        killerVal.blockOwner(blockTimeKiller);
        victimVal.blockOwner(blockTimeVictim);

        // send messages
        server.broadcastMessage(Language.placeholderReplace(lang.get("blockedBroadcast"),
                "%KILLER%", killer.getName(), "%VICTIM%", victim.getName()));
        killer.sendMessage(Language.placeholderReplace(lang.get("blockedKiller"),
                "%TIME%", String.valueOf(blockTimeKiller / 60), "%PLAYER%", victim.getName()));
        victim.sendMessage(Language.placeholderReplace(lang.get("blockedVictim"),
                "%TIME%", String.valueOf(blockTimeVictim / 60), "%PLAYER%", killer.getName()));
    }

    @Override
    public void loadConfig() {

        // Get constants
        blockTime = hms.getConfig().getInt("antiKillfarming.blockTime", 300);
        thresholdUntilBlock = hms.getConfig().getInt("antiKillfarming.thresholdUntilBlock", 5);
        thresholdUntilRemovalSeconds = hms.getConfig().getInt("antiKillfarming.thresholdUntilRemoval", 100);

        // Get allowed commands
        exemptCommands.clear();
        exemptCommands.addAll(hms.getConfig().getStringList("antiKillfarming.killfarmingCommandExemptions"));

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

    @Override
    public void sweep() {
        containerMap.values().removeIf(AntiKillfarmingContainer::sweep);
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
        final Map<UUID, Pair<Long, Integer>> players = new HashMap<>();

        int amountBlocked = 0;
        long blockedUntil = -1;

        AntiKillfarmingContainer() {}

        /**
         * Update a players timestamp and amount
         *
         * @param toUpdate - Player that will be updated
         * @param timeDiff - additional time that will be added to the threshold,
         *                 useful for making sure that after a block the players can't proceed with killfarming
         */
        void incrementPlayer(Player toUpdate, long timeDiff) {

            if (isStillValid(toUpdate.getUniqueId())) {

                Pair<Long, Integer> pair = players.get(toUpdate.getUniqueId());
                pair.setLeft((System.currentTimeMillis() / 1000) + timeDiff);
                pair.setRight(pair.getRight() + 1);

            } else players.put(toUpdate.getUniqueId(), getDefaultPair());
        }

        void removePlayer(Player toRemove) {
            players.remove(toRemove.getUniqueId());
        }

        int getAmountKilled(Player toGet) {
            return players.get(toGet.getUniqueId()).getRight();
        }

        void blockOwner(long time) {
            this.amountBlocked++;
            this.blockedUntil = System.currentTimeMillis() / 1000 + time;
        }

        void unblockOwner() {
            this.blockedUntil = -1;
        }

        long getBlockTime() {

            if (blockedUntil < 0) return -1;
            else {
                long diff = blockedUntil - (System.currentTimeMillis() / 1000);

                if (diff < 0) {
                    unblockOwner();
                    return -1;
                } else return diff;
            }
        }

        boolean sweep() {

            players.keySet().removeIf(uuid -> !isStillValid(uuid));
            return amountBlocked == 0 && players.size() == 0;
        }

        private boolean isStillValid(UUID uuid) {
            return players.containsKey(uuid)
                    && players.get(uuid).getLeft() + thresholdUntilRemovalSeconds > System.currentTimeMillis() / 1000;
        }

        private Pair<Long, Integer> getDefaultPair() {
            return new Pair<>(System.currentTimeMillis() / 1000, 1);
        }
    }
}
