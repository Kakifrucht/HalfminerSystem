package de.halfminer.hms.modules;

import de.halfminer.hms.util.Language;
import de.halfminer.hms.util.TitleSender;
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
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;

public class ModCombatLog extends HalfminerModule implements Listener {

    private final Map<String, String> lang = new HashMap<>();
    private final Map<Player, Integer> tagged = new HashMap<>();
    private boolean broadcastLog;
    private int tagTime;

    public ModCombatLog() {
        reloadConfig();
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onDeathUntag(PlayerDeathEvent e) {

        Player victim = e.getEntity().getPlayer();
        Player killer = e.getEntity().getKiller();

        untagPlayer(victim);
        if (killer != null) untagPlayer(killer);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void logoutCheckIfInCombat(PlayerQuitEvent e) {

        if (tagged.containsKey(e.getPlayer())) {
            untagPlayer(e.getPlayer());

            if (broadcastLog)
                hms.getServer().broadcast(Language.placeholderReplace(lang.get("loggedOut"), "%PLAYER%", e.getPlayer().getName()), "hms.default");

            if (e.getPlayer().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent e2 = (EntityDamageByEntityEvent) e.getPlayer().getLastDamageCause();
                if (e2.getDamager() instanceof Player)
                    untagPlayer((Player) e2.getDamager());
            }

            e.getPlayer().setHealth(0.0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onPvPTagPlayer(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {

            Player victim = (Player) e.getEntity();
            Player attacker = null;

            if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
            else if (e.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) e.getDamager();
                if (projectile.getShooter() instanceof Player) attacker = (Player) projectile.getShooter();
            }

            if (attacker != null && attacker != victim) {
                tagPlayer(victim, attacker.getName());
                tagPlayer(attacker, "");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onCommandCheckIfBlocked(PlayerCommandPreprocessEvent e) {

        if (tagged.containsKey(e.getPlayer())) {
            e.getPlayer().sendMessage(lang.get("noCommand"));
            e.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEnderpearlCheckIfBlocked(PlayerInteractEvent e) {
        if (e.hasItem() && e.getItem().getType() == Material.ENDER_PEARL && tagged.containsKey(e.getPlayer()) && ((e.getAction() == Action.RIGHT_CLICK_BLOCK) || (e.getAction() == Action.RIGHT_CLICK_AIR))) {
            e.getPlayer().sendMessage(lang.get("noEnderpearl"));
            e.getPlayer().updateInventory();
            e.setCancelled(true);
        }
    }

    private void tagPlayer(final Player p, String attacker) {

        if (p.hasPermission("hms.bypass.combatlog")) return;

        if (tagged.containsKey(p)) hms.getServer().getScheduler().cancelTask(tagged.get(p));
        else {

            if (attacker.length() > 0) {
                p.sendMessage(Language.placeholderReplace(lang.get("taggedBy"), "%PLAYER%", attacker));
            } else {
                p.sendMessage(lang.get("tagged"));
            }
        }

        int id = hms.getServer().getScheduler().runTaskTimer(hms, new Runnable() {

            final String symbols = lang.get("symbols");
            int time = tagTime;

            @Override
            public void run() {

                //build the progressbar
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

                if (time-- > 0) TitleSender.sendActionBar(p, message);
                else untagPlayer(p);
            }
        }, 0L, 20L).getTaskId();

        tagged.put(p, id);
    }

    private void untagPlayer(Player p) {

        if (!tagged.containsKey(p)) return;

        hms.getServer().getScheduler().cancelTask(tagged.get(p));
        TitleSender.sendActionBar(p, lang.get("untagged"));
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1, 2f);

        tagged.remove(p);
    }

    @Override
    public void reloadConfig() {

        broadcastLog = hms.getConfig().getBoolean("combatLog.broadcastLog", true);
        tagTime = hms.getConfig().getInt("combatLog.tagTime", 30);

        lang.clear();
        lang.put("tagged", Language.getMessagePlaceholders("modCombatLogTagged", true, "%PREFIX%", "PvP", "%TIME%", "" + tagTime));
        lang.put("taggedBy",Language.getMessagePlaceholders("modCombatLogTaggedBy", true, "%PREFIX%", "PvP", "%TIME%", "" + tagTime));
        lang.put("countdown", Language.getMessage("modCombatLogCountdown"));
        lang.put("symbols", Language.getMessage("modCombatLogProgressSymbols"));
        lang.put("untagged", Language.getMessage("modCombatLogUntagged"));
        lang.put("loggedOut", Language.getMessagePlaceholders("modCombatLogLoggedOut", true, "%PREFIX%", "PvP"));
        lang.put("noCommand", Language.getMessagePlaceholders("modCombatLogNoCommand", true, "%PREFIX%", "PvP"));
        lang.put("noEnderpearl", Language.getMessagePlaceholders("modCombatLogNoEnderpearl", true, "%PREFIX%", "PvP"));
    }
}
