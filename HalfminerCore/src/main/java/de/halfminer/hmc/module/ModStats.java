package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.handler.storage.DataType;
import de.halfminer.hms.manageable.Disableable;
import de.halfminer.hms.manageable.Sweepable;
import de.halfminer.hms.handler.storage.HalfminerPlayer;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * - Records lots of statistics about a player
 *   - Online time
 *   - Last names
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
public class ModStats extends HalfminerModule implements Disableable, Listener, Sweepable {

    private Map<Player, Long> timeOnline;
    private BukkitTask onlineTimeTask;

    private final Cache<Player, Player> lastInteract = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.SECONDS)
            .concurrencyLevel(1)
            .weakKeys()
            .weakValues()
            .build();

    private int timeUntilHomeBlockSeconds;

    @EventHandler(priority = EventPriority.LOWEST)
    public void joinInitializeStatsAndRename(PlayerJoinEvent e) {

        Player player = e.getPlayer();
        HalfminerPlayer hPlayer = storage.getPlayer(player);
        timeOnline.put(player, System.currentTimeMillis() / 1000);

        String lastName = hPlayer.getName();
        if (lastName.length() != 0 && !lastName.equalsIgnoreCase(player.getName())) {

            String lastNames = hPlayer.getString(DataType.LAST_NAMES);

            if (lastNames.length() > 0) {

                // Do not store old name if it was already used
                boolean containsName = false;
                String lastNameLowercase = lastName.toLowerCase();
                for (String str : lastNames.toLowerCase().split(" ")) {
                    if (str.equals(lastNameLowercase)) {
                        containsName = true;
                        break;
                    }
                }
                if (!containsName) hPlayer.set(DataType.LAST_NAMES, lastNames + ' ' + lastName);
            } else {
                hPlayer.set(DataType.LAST_NAMES, lastName);
            }
            MessageBuilder.create("modStatsNameChange", hmc, "Name")
                    .addPlaceholderReplace("%OLDNAME%", lastName)
                    .addPlaceholderReplace("%NEWNAME%", player.getName())
                    .broadcastMessage(true);
        }

        storage.setUUID(player);
        hPlayer.set(DataType.LAST_NAME, player.getName());

        // Votebarrier setting
        if (coreStorage.getInt("vote." + player.getUniqueId().toString()) == 0) {
            coreStorage.set("vote." + player.getUniqueId().toString(), ((System.currentTimeMillis() / 1000) + timeUntilHomeBlockSeconds));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void updatePlayerTimeLeave(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        setOnlineTime(player);
        timeOnline.remove(player);
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
                    .addPlaceholderReplace("%VICTIM%", victim.getName())
                    .addPlaceholderReplace("%KILLS%", String.valueOf(killsKiller))
                    .addPlaceholderReplace("%KDRATIO%", String.valueOf(kdRatioKiller))
                    .sendMessage(killer);



            ItemStack weapon = killer.getInventory().getItemInMainHand();
            boolean addKillingWeapon = weapon != null
                    && weapon.hasItemMeta()
                    && weapon.getItemMeta().hasDisplayName();

            MessageBuilder victimMessage = MessageBuilder.create(
                    addKillingWeapon ? "modStatsPvPDeathWeapon" : "modStatsPvPDeath", hmc, "PvP")
                    .addPlaceholderReplace("%KILLER%", killer.getName());

            if (addKillingWeapon) {
                victimMessage.addPlaceholderReplace("%WEAPON%", weapon.getItemMeta().getDisplayName());
            }

            victimMessage.sendMessage(victim);

            MessageBuilder.create("modStatsPvPLog", hmc)
                    .addPlaceholderReplace("%KILLER%", killer.getName())
                    .addPlaceholderReplace("%VICTIM%", victim.getName())
                    .logMessage(Level.INFO);
        } else {

            hVictim.incrementInt(DataType.DEATHS, 1);
            hVictim.set(DataType.KD_RATIO, calculateKDRatio(hVictim));

            MessageBuilder.create("modStatsDeath", hmc, "PvP")
                    .addPlaceholderReplace("%DEATHS%", hVictim.getString(DataType.DEATHS))
                    .sendMessage(victim);

            MessageBuilder.create("modStatsDeathLog", hmc)
                    .addPlaceholderReplace("%PLAYER%", victim.getName())
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
        String kills = String.valueOf(hClicked.getInt(DataType.KILLS));
        String kdratio = String.valueOf(hClicked.getDouble(DataType.KD_RATIO));

        MessageBuilder.create(!clicked.hasPermission("hmc.bypass.statsrightclick") ?
                "modStatsRightClick" : "modStatsRightClickExempt", hmc, clicked.getName())
                .addPlaceholderReplace("%SKILLGROUP%", skillgroup)
                .addPlaceholderReplace("%KILLS%", kills)
                .addPlaceholderReplace("%KDRATIO%", kdratio)
                .addPlaceholderReplace("%AFK%", hookHandler.isAfk(clicked) ?
                        MessageBuilder.returnMessage("modStatsRightClickAFKAppend", hmc) : "")
                .sendMessage(clicker);

        clicker.playSound(clicked.getLocation(), Sound.BLOCK_SLIME_HIT, 1.0f, 2.0f);
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

    private void setOnlineTime(Player player) {

        if (timeOnline.containsKey(player)) {
            long lastTime = timeOnline.get(player);
            long currentTime = System.currentTimeMillis() / 1000;
            int time = (int) (currentTime - lastTime);

            storage.getPlayer(player).incrementInt(DataType.TIME_ONLINE, time);
            timeOnline.put(player, currentTime);
        }
    }

    @Override
    public void loadConfig() {

        timeUntilHomeBlockSeconds = hmc.getConfig().getInt("command.home.timeUntilHomeBlockMinutes") * 60;

        if (timeOnline == null) {
            timeOnline = new HashMap<>();
            long time = System.currentTimeMillis() / 1000;
            for (Player player : server.getOnlinePlayers()) {
                timeOnline.put(player, time);
            }
        }

        if (onlineTimeTask == null) {
            onlineTimeTask = scheduler.runTaskTimer(hmc, () ->
                    server.getOnlinePlayers().forEach(this::setOnlineTime), 1200L, 1200L);
        }
    }

    @Override
    public void onDisable() {
        server.getOnlinePlayers().forEach(this::setOnlineTime);
    }

    @Override
    public void sweep() {
        lastInteract.cleanUp();
    }
}
