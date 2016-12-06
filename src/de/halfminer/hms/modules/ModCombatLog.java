package de.halfminer.hms.modules;

import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

/**
 * - Tags players when hitting/being hit
 * - Combatlogging causes instant death
 * - Shows actionbar message containing time left in fight
 * - Untags players after timer runs out, player logs out or a player is killed
 * - Disables during fight:
 *   - Taking off armor
 *   - Commands
 *   - Enderpearls
 */
public class ModCombatLog extends HalfminerModule implements Listener {

    private final Map<Player, BukkitTask> tagged = Collections.synchronizedMap(new HashMap<>());

    private boolean broadcastLog;
    private int tagTime;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathUntag(PlayerDeathEvent e) {

        Player victim = e.getEntity().getPlayer();
        Player killer = e.getEntity().getKiller();

        untagPlayer(victim);
        if (killer != null) {
            untagPlayer(killer);
        }
    }

    @EventHandler
    public void logoutCheckIfInCombat(PlayerQuitEvent e) {

        if (isTagged(e.getPlayer())) {
            untagPlayer(e.getPlayer());

            if (broadcastLog)
                MessageBuilder.create(hms, "modCombatLogLoggedOut", "PvP")
                        .addPlaceholderReplace("%PLAYER%", e.getPlayer().getName())
                        .broadcastMessage(true);

            if (e.getPlayer().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e2 = (EntityDamageByEntityEvent) e.getPlayer().getLastDamageCause();
                if (e2.getDamager() instanceof Player)
                    untagPlayer((Player) e2.getDamager());
            }

            e.getPlayer().setHealth(0.0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPTagPlayer(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {

            Player victim = (Player) e.getEntity();
            Player attacker = null;

            if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
            else if (e.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) e.getDamager();
                if (projectile.getShooter() instanceof Player) attacker = (Player) projectile.getShooter();
            }

            if (attacker != null && attacker != victim
                    && !attacker.isDead() && !victim.isDead()) {
                tagPlayer(victim);
                tagPlayer(attacker);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClickDenyArmorChange(InventoryClickEvent e) {

        if (isTagged((Player) e.getWhoClicked())
                && e.getSlot() >= 36
                && e.getSlot() <= 39
                && e.getCurrentItem() != null
                && !e.getCurrentItem().getType().equals(Material.AIR)) {
            MessageBuilder.create(hms, "modCombatLogNoArmorChange", "PvP").sendMessage(e.getWhoClicked());
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommandCheckIfBlocked(PlayerCommandPreprocessEvent e) {

        if (isTagged(e.getPlayer())) {
            MessageBuilder.create(hms, "modCombatLogNoCommand", "PvP").sendMessage(e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderpearlCheckIfBlocked(PlayerInteractEvent e) {

        if (isTagged(e.getPlayer()) && e.hasItem() && e.getItem().getType() == Material.ENDER_PEARL
                && ((e.getAction() == Action.RIGHT_CLICK_BLOCK) || (e.getAction() == Action.RIGHT_CLICK_AIR))) {
            MessageBuilder.create(hms, "modCombatLogNoEnderpearl", "PvP").sendMessage(e.getPlayer());
            e.getPlayer().updateInventory();
            e.setCancelled(true);
        }
    }

    private void tagPlayer(final Player p) {

        if (p.hasPermission("hms.bypass.combatlog")) return;

        if (isTagged(p)) tagged.get(p).cancel();
        tagged.put(p, scheduler.runTaskTimerAsynchronously(hms, new Runnable() {

            final String symbols = MessageBuilder.returnMessage(hms, "modCombatLogProgressSymbols");
            int time = tagTime;

            @Override
            public void run() {
                // build the progressbar
                int timePercentage = (int) Math.round((time / (double) tagTime) * 10);
                String progressBar = "" + ChatColor.DARK_RED + ChatColor.STRIKETHROUGH;
                boolean switchedColors = false;
                for (int i = 0; i < 10; i++) {
                    if (timePercentage-- < 1 && !switchedColors) {
                        progressBar += "" + ChatColor.GRAY + ChatColor.STRIKETHROUGH;
                        switchedColors = true; //only append color code once
                    }
                    progressBar += symbols;
                }

                if (time-- > 0) titleHandler.sendActionBar(p, MessageBuilder.create(hms, "modCombatLogCountdown")
                        .addPlaceholderReplace("%TIME%", String.valueOf(time + 1))
                        .addPlaceholderReplace("%PROGRESSBAR%", progressBar)
                        .returnMessage());
                else untagPlayer(p);
            }
        }, 0L, 20L));
    }

    private void untagPlayer(Player p) {

        if (!isTagged(p)) return;

        tagged.get(p).cancel();
        titleHandler.sendActionBar(p, MessageBuilder.returnMessage(hms, "modCombatLogUntagged"));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 2f);

        tagged.remove(p);
    }

    boolean isTagged(Player p) {
        return tagged.containsKey(p);
    }

    @Override
    public void loadConfig() {

        broadcastLog = hms.getConfig().getBoolean("combatLog.broadcastLog", true);
        tagTime = hms.getConfig().getInt("combatLog.tagTime", 15);
    }
}
