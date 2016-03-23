package de.halfminer.hms.modules;

import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanBossBar;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.Language;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
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
 * Disables the possibility to logout during combat
 * - Tags players when hitting/being hit
 * - Shows health and name of attacker/victim via BossBar
 * - Combatlogging causes instant death
 * - Shows titles containing time left in fight
 * - Untags players after timer runs out, player logs out or a player is killed
 * - Disables armor switching, commands and enderpearls from being used during fight
 */
@SuppressWarnings("unused")
public class ModCombatLog extends HalfminerModule implements Listener {

    private final HanTitles titleHandler = (HanTitles) hms.getHandler(HandlerType.TITLES);
    private final HanBossBar barHandler = (HanBossBar) hms.getHandler(HandlerType.BOSSBAR);

    private final Map<String, String> lang = new HashMap<>();
    private final Map<Player, BukkitTask> tagged = Collections.synchronizedMap(new HashMap<Player, BukkitTask>());

    private boolean broadcastLog;
    private int tagTime;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeathUntag(PlayerDeathEvent e) {

        Player victim = e.getEntity().getPlayer();
        Player killer = e.getEntity().getKiller();

        untagPlayer(victim);
        if (killer != null) untagPlayer(killer);
    }

    @EventHandler
    public void logoutCheckIfInCombat(PlayerQuitEvent e) {

        if (tagged.containsKey(e.getPlayer())) {
            untagPlayer(e.getPlayer());

            if (broadcastLog)
                server.broadcast(Language.placeholderReplace(lang.get("loggedOut"), "%PLAYER%", e.getPlayer().getName()), "hms.default");

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

            if (attacker != null && attacker != victim && !attacker.isDead() && !victim.isDead()) {
                tagPlayer(victim, attacker, (int) attacker.getHealth());
                tagPlayer(attacker, victim, (int) Math.ceil(victim.getHealth() - e.getFinalDamage()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClickDenyArmorChange(InventoryClickEvent e) {

        if (isTagged((Player) e.getWhoClicked())
                && e.getSlot() >= 36 && e.getSlot() <= 39) {
            e.getWhoClicked().sendMessage(lang.get("noArmorChange"));
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommandCheckIfBlocked(PlayerCommandPreprocessEvent e) {

        if (isTagged(e.getPlayer())) {
            e.getPlayer().sendMessage(lang.get("noCommand"));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnderpearlCheckIfBlocked(PlayerInteractEvent e) {

        if (isTagged(e.getPlayer()) && e.hasItem() && e.getItem().getType() == Material.ENDER_PEARL
                && ((e.getAction() == Action.RIGHT_CLICK_BLOCK) || (e.getAction() == Action.RIGHT_CLICK_AIR))) {
            e.getPlayer().sendMessage(lang.get("noEnderpearl"));
            e.getPlayer().updateInventory();
            e.setCancelled(true);
        }
    }

    private void tagPlayer(final Player p, final Player other, int otherHealth) {

        if (p.hasPermission("hms.bypass.combatlog")) return;

        final int health = otherHealth >= 0 ? otherHealth : 0;
        final int healthScale = (int) other.getMaxHealth();

        barHandler.sendBar(p, Language.placeholderReplace(lang.get("bossbar"), "%PLAYER%", other.getName(),
                "%HEALTH%", String.valueOf(health), "%MAXHEALTH%", String.valueOf(healthScale)),
                BarColor.RED, BarStyle.SEGMENTED_20, 8, (double) health / healthScale);

        if (isTagged(p)) tagged.get(p).cancel();
        tagged.put(p, scheduler.runTaskTimerAsynchronously(hms, new Runnable() {

            final String symbols = lang.get("symbols");
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

                String message = Language.placeholderReplace(lang.get("countdown"), "%TIME%", String.valueOf(time),
                        "%PROGRESSBAR%", progressBar);

                if (time-- > 0) titleHandler.sendActionBar(p, message);
                else untagPlayer(p);
            }
        }, 0L, 20L));
    }

    private void untagPlayer(Player p) {

        if (!isTagged(p)) return;

        tagged.get(p).cancel();
        titleHandler.sendActionBar(p, lang.get("untagged"));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 2f);

        tagged.remove(p);
    }

    private boolean isTagged(Player p) {
        return tagged.containsKey(p);
    }

    @Override
    public void reloadConfig() {

        broadcastLog = hms.getConfig().getBoolean("combatLog.broadcastLog", true);
        tagTime = hms.getConfig().getInt("combatLog.tagTime", 15);

        lang.put("bossbar", Language.getMessage("modCombatLogBossBar"));
        lang.put("countdown", Language.getMessage("modCombatLogCountdown"));
        lang.put("symbols", Language.getMessage("modCombatLogProgressSymbols"));
        lang.put("untagged", Language.getMessage("modCombatLogUntagged"));
        lang.put("loggedOut", Language.getMessagePlaceholders("modCombatLogLoggedOut", true, "%PREFIX%", "PvP"));
        lang.put("noArmorChange", Language.getMessagePlaceholders("modCombatLogNoArmorChange", true, "%PREFIX%", "PvP"));
        lang.put("noCommand", Language.getMessagePlaceholders("modCombatLogNoCommand", true, "%PREFIX%", "PvP"));
        lang.put("noEnderpearl", Language.getMessagePlaceholders("modCombatLogNoEnderpearl", true, "%PREFIX%", "PvP"));
    }
}
