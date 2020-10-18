package de.halfminer.hmc.module;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.ReflectUtils;
import de.halfminer.hms.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * - Tags players when hitting/being hit
 * - Shows actionbar message containing time left in fight
 * - On logout
 *   - Combat logging player dies
 *   - Last attacker will get the kill and get untagged
 *   - Message will be broadcast, containing last attacker
 * - Untags players after timer runs out, player logs out or a player is killed
 * - Players that are tagged can fight in PvP disabled regions aswell (togglable, prevents border hopping)
 * - Disables during fight:
 *   - Taking off armor
 *   - Commands
 *   - Enderpearls
 */
public class ModCombatLog extends HalfminerModule implements Listener {

    private final Map<Player, BukkitTask> tagged = Collections.synchronizedMap(new HashMap<>());
    private Cache<Player, Player> lastOpponentCache;

    private boolean broadcastLog;
    private boolean preventBorderHopping;
    private int tagTime;


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathUntag(PlayerDeathEvent e) {

        Player victim = e.getEntity();
        Player killer = e.getEntity().getKiller();

        untagPlayer(victim);
        if (killer != null) {
            untagPlayer(killer);
        } else {
            // untag last opponent if the last opponent of victim's last opponent is the victim aswell
            // this prevents untagging a player if they are currently fighting with a different player
            Player lastOpponent = lastOpponentCache.getIfPresent(victim);
            if (lastOpponent != null) {
                Player lastOpponentOfOpponent = lastOpponentCache.getIfPresent(lastOpponent);
                if (victim.equals(lastOpponentOfOpponent)) {
                    untagPlayer(lastOpponent);
                }
            }
        }
    }

    @EventHandler
    public void logoutCheckIfInCombat(PlayerQuitEvent e) {

        Player p = e.getPlayer();
        if (isTagged(p) && !p.isDead()) {

            untagPlayer(p);
            Player lastOpponent = lastOpponentCache.getIfPresent(p);
            // cannot be null logically
            if (lastOpponent != null) {

                // untag last damaging player, ensure that last player gets the kill
                untagPlayer(lastOpponent);
                ReflectUtils.setKiller(p, lastOpponent);

                if (broadcastLog) {
                    Message.create("modCombatLogLoggedOut", hmc, "PvP")
                            .addPlaceholder("%PLAYER%", p.getName())
                            .addPlaceholder("%ENEMY%", lastOpponent.getName())
                            .broadcast(true);
                }
            }
            p.setHealth(0.0d);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPTagPlayer(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {

            Player victim = (Player) e.getEntity();
            Player attacker = Utils.getPlayerSourceFromEntity(e.getDamager());

            if (!victim.equals(attacker) && isPvP(victim, attacker)) {

                lastOpponentCache.put(victim, attacker);
                lastOpponentCache.put(attacker, victim);
                tagPlayer(victim);
                tagPlayer(attacker);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvPUncancelCancelled(EntityDamageByEntityEvent e) {
        uncancelDamageEvent(e, e.getDamager(), e.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvPCombustUncancel(EntityCombustByEntityEvent e) {
        uncancelDamageEvent(e, e.getCombuster(), e.getEntity());
    }

    private void uncancelDamageEvent(Cancellable event, Entity attackerEntity, Entity victimEntity) {
        if (preventBorderHopping
                && event.isCancelled()
                && victimEntity instanceof Player) {

            Player attacker = Utils.getPlayerSourceFromEntity(attackerEntity);
            Player victim = (Player) victimEntity;

            if (isPvP(victim, attacker)
                    && isTagged(attacker)
                    && isTagged(victim)) {
                event.setCancelled(false);
            }
        }
    }

    private boolean isPvP(Player victim, Player attacker) {
        return attacker != null
                && attacker != victim
                && !attacker.isDead()
                && !victim.isDead();
    }

    @EventHandler(ignoreCancelled = true)
    public void onClickDenyArmorChange(InventoryClickEvent e) {

        if (isTagged((Player) e.getWhoClicked())
                && e.getSlot() >= 36
                && e.getSlot() <= 39
                && e.getCurrentItem() != null
                && !e.getCurrentItem().getType().equals(Material.AIR)) {
            Message.create("modCombatLogNoArmorChange", hmc, "PvP").send(e.getWhoClicked());
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommandCheckIfBlocked(PlayerCommandPreprocessEvent e) {

        if (isTagged(e.getPlayer())) {
            Message.create("modCombatLogNoCommand", hmc, "PvP").send(e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderpearlCheckIfBlocked(PlayerInteractEvent e) {

        if (isTagged(e.getPlayer()) && e.hasItem() && e.getItem().getType() == Material.ENDER_PEARL
                && ((e.getAction() == Action.RIGHT_CLICK_BLOCK) || (e.getAction() == Action.RIGHT_CLICK_AIR))) {
            Message.create("modCombatLogNoEnderpearl", hmc, "PvP").send(e.getPlayer());
            e.getPlayer().updateInventory();
            e.setCancelled(true);
        }
    }

    private void tagPlayer(final Player p) {

        if (p.hasPermission("hmc.bypass.combatlog")) return;

        if (isTagged(p)) tagged.get(p).cancel();
        tagged.put(p, scheduler.runTaskTimerAsynchronously(hmc, new Runnable() {

            final String symbols = Message.returnMessage("modCombatLogProgressSymbols", hmc);
            int time = tagTime;

            @Override
            public void run() {
                // build the progressbar
                int timePercentage = (int) Math.round((time / (double) tagTime) * 10);
                StringBuilder progressBar = new StringBuilder("" + ChatColor.DARK_RED + ChatColor.STRIKETHROUGH);
                boolean switchedColors = false;
                for (int i = 0; i < 10; i++) {
                    if (timePercentage-- < 1 && !switchedColors) {
                        progressBar.append(ChatColor.GRAY).append(ChatColor.STRIKETHROUGH);
                        switchedColors = true; // only append color code once
                    }
                    progressBar.append(symbols);
                }

                if (time-- > 0) titleHandler.sendActionBar(p, Message.create("modCombatLogCountdown", hmc)
                        .addPlaceholder("%TIME%", time + 1)
                        .addPlaceholder("%PROGRESSBAR%", progressBar.toString())
                        .returnMessage());
                else untagPlayer(p);
            }
        }, 0L, 20L));
    }

    private void untagPlayer(Player p) {

        if (!isTagged(p)) return;

        tagged.get(p).cancel();
        titleHandler.sendActionBar(p, Message.returnMessage("modCombatLogUntagged", hmc));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 2f);

        tagged.remove(p);
    }

    boolean isTagged(Player p) {
        return tagged.containsKey(p);
    }

    @Override
    public void loadConfig() {

        broadcastLog = hmc.getConfig().getBoolean("combatLog.broadcastLog", true);
        preventBorderHopping = hmc.getConfig().getBoolean("combatLog.preventBorderHopping", true);
        tagTime = hmc.getConfig().getInt("combatLog.tagTime", 15);

        lastOpponentCache = Utils.copyValues(lastOpponentCache,
                CacheBuilder.newBuilder()
                        .weakKeys()
                        .expireAfterWrite(tagTime, TimeUnit.SECONDS)
                        .build());
    }
}
