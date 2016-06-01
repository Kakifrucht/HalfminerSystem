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

        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();

        // direct PvP check
        if (e.getDamager() instanceof Player)
            e.setCancelled(checkTimeAndMessage((Player) e.getDamager(), "noPvPAttack")
                    || checkTimeAndMessage(victim, "noPvPProtect", (Player) e.getDamager()));

        // prevent death by tnt ignition
        if (e.getDamager() instanceof TNTPrimed
                && ((TNTPrimed) e.getDamager()).getSource() instanceof Player)
            e.setCancelled(checkTimeAndMessage(victim, ""));

        // arrow hit check
        if (e.getDamager() instanceof Projectile
                && ((Projectile) e.getDamager()).getShooter() instanceof Player) {

            Player shooter = (Player) ((Projectile) e.getDamager()).getShooter();

            e.setCancelled(checkTimeAndMessage(shooter, "noPvPAttack")
                    || checkTimeAndMessage(victim, "noPvPProtect", shooter));
        }
    }

    private boolean checkTimeAndMessage(Player toCheck, String messageKey) {
        return checkTimeAndMessage(toCheck, messageKey, toCheck);
    }

    private boolean checkTimeAndMessage(Player toCheck, String messageKey, Player toMessage) {
        int blockTime = getBlockTime(toCheck);
        if (blockTime >= 0) {
            if (messageKey.length() > 0)
                toMessage.sendMessage(Language.placeholderReplace(lang.get(messageKey),
                        "%PLAYER%", toCheck.getName(), "%TIME%", String.valueOf(blockTime)));
            return true;
        }
        return false;
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

        return e.getEntity().getShooter() instanceof Player
                && checkTimeAndMessage((Player) e.getEntity().getShooter(), "noPvPAttack");
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
        AntiKillfarmingContainer killerCon = containerMap.get(killer.getUniqueId());
        AntiKillfarmingContainer victimCon;
        if (!containerMap.containsKey(victim.getUniqueId())) {
            victimCon = new AntiKillfarmingContainer();
            containerMap.put(victim.getUniqueId(), victimCon);
        } else victimCon = containerMap.get(victim.getUniqueId());

        int blockTimeKiller = killerCon.blockOwner();
        int blockTimeVictim = victimCon.blockOwner();

        // ensure that once both blocks run out they get reblocked when trying to kill again
        killerCon.incrementPlayer(victim, blockTimeKiller > blockTimeVictim ? blockTimeKiller : blockTimeVictim);

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

            } else players.put(toUpdate.getUniqueId(), new Pair<>(System.currentTimeMillis() / 1000, 1));
        }

        void removePlayer(Player toRemove) {
            players.remove(toRemove.getUniqueId());
        }

        int getAmountKilled(Player toGet) {
            return players.get(toGet.getUniqueId()).getRight();
        }

        int blockOwner() {

            amountBlocked++;

            int blockTimeNow = blockTime;
            for (int i = 1; i < amountBlocked; i++) blockTimeNow *= 2;
            this.blockedUntil = System.currentTimeMillis() / 1000 + blockTimeNow;

            return blockTimeNow;
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
    }
}
