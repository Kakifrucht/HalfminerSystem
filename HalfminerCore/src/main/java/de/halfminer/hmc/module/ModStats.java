package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * - Records lots of additional statistics about a player
 *   - Kill/death count
 *   - K/D ratio
 *   - Blocks placed/broken
 *   - Mobkills
 *   - Money earned
 * - View stats on rightclicking a player
 *   - Exempt permission
 *   - Show if player is AFK
 */
@SuppressWarnings("unused")
public class ModStats extends HalfminerModule implements Listener, Sweepable {

    private final Cache<Player, Player> lastInteract = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .concurrencyLevel(1)
            .weakKeys()
            .weakValues()
            .build();

    private int timeUntilHomeBlockSeconds;


    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinSetVoteBarrier(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        if (coreStorage.getInt("vote." + player.getUniqueId().toString()) == 0) {
            coreStorage.set("vote." + player.getUniqueId().toString(), ((System.currentTimeMillis() / 1000) + timeUntilHomeBlockSeconds));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void deathStatsUpdateAndMessage(PlayerDeathEvent e) {

        Player killer = e.getEntity().getKiller();
        Player victim = e.getEntity();

        HalfminerPlayer hVictim = storage.getPlayer(victim);

        if (killer != null && victim != killer) {

            HalfminerPlayer hKiller = storage.getPlayer(killer);

            int killsKiller = hKiller.incrementInt(DataType.KILLS, 1);
            int deathsVictim = hVictim.incrementInt(DataType.DEATHS, 1);

            double kdRatioKiller = calculateKDRatio(hKiller);
            double kdRatioVictim = calculateKDRatio(hVictim);
            hKiller.set(DataType.KD_RATIO, kdRatioKiller);
            hVictim.set(DataType.KD_RATIO, kdRatioVictim);

            MessageBuilder.create("modStatsPvPKill", hmc, "PvP")
                    .addPlaceholder("%VICTIM%", victim.getName())
                    .addPlaceholder("%KILLS%", killsKiller)
                    .addPlaceholder("%KDRATIO%", kdRatioKiller)
                    .sendMessage(killer);

            ItemStack weapon = killer.getInventory().getItemInMainHand();
            boolean addKillingWeapon = weapon.hasItemMeta()
                    && weapon.getItemMeta().hasDisplayName();

            MessageBuilder victimMessage = MessageBuilder.create(
                    addKillingWeapon ? "modStatsPvPDeathWeapon" : "modStatsPvPDeath", hmc, "PvP")
                    .addPlaceholder("%KILLER%", killer.getName());

            if (addKillingWeapon) {
                victimMessage.addPlaceholder("%WEAPON%", weapon.getItemMeta().getDisplayName());
            }

            victimMessage.sendMessage(victim);

            MessageBuilder.create("modStatsPvPLog", hmc)
                    .addPlaceholder("%KILLER%", killer.getName())
                    .addPlaceholder("%VICTIM%", victim.getName())
                    .logMessage(Level.INFO);
        } else {

            hVictim.incrementInt(DataType.DEATHS, 1);
            hVictim.set(DataType.KD_RATIO, calculateKDRatio(hVictim));

            MessageBuilder.create("modStatsDeath", hmc, "PvP")
                    .addPlaceholder("%DEATHS%", hVictim.getString(DataType.DEATHS))
                    .sendMessage(victim);

            MessageBuilder.create("modStatsDeathLog", hmc)
                    .addPlaceholder("%PLAYER%", victim.getName())
                    .addPlaceholder("%CAUSE%", victim.getLastDamageCause().getCause().toString())
                    .logMessage(Level.INFO);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void interactShowStats(PlayerInteractEntityEvent e) {

        if (!(e.getRightClicked() instanceof Player)) return;

        Player clicker = e.getPlayer();
        Player clicked = (Player) e.getRightClicked();

        if (((ModCombatLog) hmc.getModule(ModuleType.COMBAT_LOG)).isTagged(clicker)
                || lastInteract.getIfPresent(clicker) == clicked) return;

        HalfminerPlayer hClicked = storage.getPlayer(clicked);
        String skillgroup = ((ModSkillLevel) hmc.getModule(ModuleType.SKILL_LEVEL)).getSkillgroup(clicked);

        MessageBuilder.create(!clicked.hasPermission("hmc.bypass.statsrightclick") ?
                "modStatsRightClick" : "modStatsRightClickExempt", hmc, clicked.getName())
                .addPlaceholder("%SKILLGROUP%", skillgroup)
                .addPlaceholder("%KILLS%", hClicked.getInt(DataType.KILLS))
                .addPlaceholder("%KDRATIO%", hClicked.getDouble(DataType.KD_RATIO))
                .addPlaceholder("%AFK%", hookHandler.isAfk(clicked) ?
                        MessageBuilder.returnMessage("modStatsRightClickAFKAppend", hmc) : "")
                .sendMessage(clicker);

        clicker.playSound(clicked.getLocation(), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 0.5f, 1.9f);
        lastInteract.put(clicker, clicked);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void mobkillStats(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Player) && e.getEntity().getKiller() != null) {
            storage.getPlayer(e.getEntity().getKiller()).incrementInt(DataType.MOB_KILLS, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockPlaceStats(BlockPlaceEvent e) {
        storage.getPlayer(e.getPlayer()).incrementInt(DataType.BLOCKS_PLACED, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockBreakStats(BlockBreakEvent e) {
        storage.getPlayer(e.getPlayer()).incrementInt(DataType.BLOCKS_BROKEN, 1);
    }

    private double calculateKDRatio(HalfminerPlayer player) {

        int deaths = player.getInt(DataType.DEATHS);
        if (deaths == 0) {
            return 999999.99d;
        }

        double calc = player.getInt(DataType.KILLS) / (double) deaths;
        return Utils.roundDouble(calc);
    }

    @Override
    public void loadConfig() {
        timeUntilHomeBlockSeconds = hmc.getConfig().getInt("command.home.timeUntilHomeBlockMinutes") * 60;
    }

    @Override
    public void sweep() {
        lastInteract.cleanUp();
    }
}
