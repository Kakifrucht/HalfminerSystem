package de.halfminer.hms.modules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.interfaces.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.Utils;
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
import java.util.concurrent.TimeUnit;

/**
 * - Counts amount of kills between two players
 * - After set amount of kills has been reached, blocks players for a set amount of time
 *   - Checks interval between kills, resets if interval exceeds given amount
 *   - Broadcasts block to all players
 *     - Pre warns players one kill before they get blocked
 *     - Also prints informational message that killfarming is not allowed
 *   - Blocks further PvP (both denies for blocked players and prevent other players from hitting aswell)
 *     - Direct hitting
 *     - TnT killing
 *     - Arrow shooting
 *     - Splash/Lingering potion throwing
 *   - Blocks commands
 *   - Prints message with remaining block time
 * - Punishment doubles for every additional block
 */
@SuppressWarnings("unused")
public class ModAntiKillfarming extends HalfminerModule implements Listener, Sweepable {

    private int blockTime;
    private int thresholdUntilBlock;
    private int thresholdUntilRemovalSeconds;

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
                MessageBuilder.create(hms, "modAntiKillfarmingWarning", "PvP").sendMessage(killer, victim);
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
            e.setCancelled(checkTimeAndMessage((Player) e.getDamager(), "modAntiKillfarmingNoPvPAttack")
                    || checkTimeAndMessage(victim, "modAntiKillfarmingNoPvPProtect", (Player) e.getDamager()));

        // prevent death by tnt ignition
        if (e.getDamager() instanceof TNTPrimed
                && ((TNTPrimed) e.getDamager()).getSource() instanceof Player)
            e.setCancelled(checkTimeAndMessage(victim, ""));

        // arrow hit check
        if (e.getDamager() instanceof Projectile
                && ((Projectile) e.getDamager()).getShooter() instanceof Player) {

            Player shooter = (Player) ((Projectile) e.getDamager()).getShooter();

            e.setCancelled(checkTimeAndMessage(shooter, "modAntiKillfarmingNoPvPAttack")
                    || checkTimeAndMessage(victim, "modAntiKillfarmingNoPvPProtect", shooter));
        }
    }

    private boolean checkTimeAndMessage(Player toCheck, String messageKey) {
        return checkTimeAndMessage(toCheck, messageKey, toCheck);
    }

    private boolean checkTimeAndMessage(Player toCheck, String messageKey, Player toMessage) {
        int blockTime = getBlockTime(toCheck);
        if (blockTime >= 0) {
            if (messageKey.length() > 0)
                MessageBuilder.create(hms, messageKey, "PvP")
                        .addPlaceholderReplace("%PLAYER%", toCheck.getName())
                        .addPlaceholderReplace("%TIME%", String.valueOf(blockTime))
                        .sendMessage(toMessage);
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
                MessageBuilder.create(hms, "modAntiKillfarmingNoCommand", "PvP")
                        .addPlaceholderReplace("%TIME%", String.valueOf(time))
                        .sendMessage(e.getPlayer());
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
                && checkTimeAndMessage((Player) e.getEntity().getShooter(), "modAntiKillfarmingNoPvPAttack");
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

        // ensure that once both blocks run out they get reblocked when trying to kill directly again
        killerCon.incrementPlayer(victim, blockTimeKiller > blockTimeVictim ? blockTimeKiller : blockTimeVictim);

        MessageBuilder.create(hms, "modAntiKillfarmingBlockedBroadcast", "PvP")
                .addPlaceholderReplace("%KILLER%", killer.getName())
                .addPlaceholderReplace("%VICTIM%", victim.getName())
                .broadcastMessage(true);

        MessageBuilder.create(hms, "modAntiKillfarmingBlockedKiller", "PvP")
                .addPlaceholderReplace("%TIME%", String.valueOf(blockTimeKiller / 60))
                .addPlaceholderReplace("%PLAYER%", victim.getName())
                .sendMessage(killer);

        MessageBuilder.create(hms, "modAntiKillfarmingBlockedVictim", "PvP")
                .addPlaceholderReplace("%TIME%", String.valueOf(blockTimeVictim / 60))
                .addPlaceholderReplace("%PLAYER%", killer.getName())
                .sendMessage(victim);
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

        containerMap.values().forEach(AntiKillfarmingContainer::reloadCache);
    }

    @Override
    public void sweep() {
        containerMap.values().removeIf(AntiKillfarmingContainer::sweep);
    }

    /**
     * Class bundling all information about who got killed, how often, when,
     * how often the owner was blocked already and if he is blocked right now
     */
    private class AntiKillfarmingContainer {

        /**
         * Cache containing the players that got killed by the containers owner
         * Pair mapping: Left - Timestamp, Right - Counter (amount of kills)
         */
        Cache<UUID, Pair<Long, Integer>> playerCache;

        int amountBlocked = 0;
        long blockedUntil = -1;

        AntiKillfarmingContainer() {
            reloadCache();
        }

        /**
         * Update a players timestamp and amount
         *
         * @param toUpdate player that will be updated
         * @param timeDiff additional time that will be added to the threshold,
         *                 useful for making sure that after a block the players can't proceed with killfarming
         */
        void incrementPlayer(Player toUpdate, long timeDiff) {

            Pair<Long, Integer> playerInfo = playerCache.getIfPresent(toUpdate.getUniqueId());

            if (playerInfo != null) {

                playerInfo.setLeft((System.currentTimeMillis() / 1000) + timeDiff);
                playerInfo.setRight(playerInfo.getRight() + 1);

            } else playerCache.put(toUpdate.getUniqueId(), new Pair<>(System.currentTimeMillis() / 1000, 1));
        }

        void removePlayer(Player toRemove) {
            playerCache.invalidate(toRemove.getUniqueId());
        }

        int getAmountKilled(Player toGet) {

            Pair<Long, Integer> playerInfo = playerCache.getIfPresent(toGet.getUniqueId());

            if (playerInfo != null) return playerInfo.getRight();
            else return 0;
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
            playerCache.cleanUp();
            return amountBlocked == 0 && playerCache.size() == 0;
        }

        void reloadCache() {
            playerCache = Utils.getNewCache(playerCache,
                    CacheBuilder.newBuilder()
                            .concurrencyLevel(1)
                            .expireAfterWrite(thresholdUntilRemovalSeconds, TimeUnit.SECONDS)
                            .build(), null, "");
        }
    }
}
