package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
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

public class ModCombatLog implements HalfminerModule, Listener {

    private final static HalfminerSystem hms = HalfminerSystem.getInstance();
    private final Map<String, String> lang = new HashMap<>();
    private final Map<Player, Integer> tagged = new HashMap<>();
    private boolean broadcastLog;
    private int tagTime;

    public ModCombatLog() {
        reloadConfig();
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onDeath(PlayerDeathEvent e) {
        if (tagged.containsKey(e.getEntity().getPlayer()))
            untagPlayer(e.getEntity().getPlayer(), false);
        if (tagged.containsKey(e.getEntity().getKiller()))
            untagPlayer(e.getEntity().getKiller(), true);
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onLogout(PlayerQuitEvent e) {
        if (tagged.containsKey(e.getPlayer())) {
            untagPlayer(e.getPlayer(), false);

            if (broadcastLog)
                hms.getServer().broadcast(Language.placeholderReplace(lang.get("loggedOut"), "%PLAYER%", e.getPlayer().getName()), "hms.default");

            EntityDamageByEntityEvent e2 = (EntityDamageByEntityEvent) e.getPlayer().getLastDamageCause();

            if (e2 != null && e2.getDamager() instanceof Player)
                untagPlayer((Player) e2.getDamager(), true);

            e.getPlayer().setHealth(0.0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onPvP(EntityDamageByEntityEvent e) {

        if (e.getEntity() instanceof Player) {

            Player victim = (Player) e.getEntity();
            Player attacker = null;

            if (e.getDamager() instanceof Player) attacker = (Player) e.getDamager();
            else if (e.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) e.getDamager();
                if (projectile.getShooter() instanceof Player) attacker = (Player) projectile.getShooter();
            }
            if (attacker != null && attacker != victim) {
                tagPlayer(victim);
                tagPlayer(attacker);
            }
        }

    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (tagged.containsKey(e.getPlayer())) {
            e.getPlayer().sendMessage(lang.get("noCommand"));
            e.setCancelled(true);
        }
    }

    @EventHandler
    @SuppressWarnings("unused")
    public void onEnderpearl(PlayerInteractEvent e) {
        if (e.hasItem() && e.getItem().getType() == Material.ENDER_PEARL && tagged.containsKey(e.getPlayer()) && ((e.getAction() == Action.RIGHT_CLICK_BLOCK) || (e.getAction() == Action.RIGHT_CLICK_AIR))) {
            e.getPlayer().sendMessage(lang.get("noEnderpearl"));
            e.getPlayer().updateInventory();
            e.setCancelled(true);
        }
    }

    private void tagPlayer(final Player p) {

        if (p.isOp() || p.hasPermission("hms.bypasscombatlog")) return;

        if (tagged.containsKey(p)) hms.getServer().getScheduler().cancelTask(tagged.get(p));
        else p.sendMessage(lang.get("tagged"));

        int id = hms.getServer().getScheduler().scheduleSyncDelayedTask(hms, new Runnable() {
            @Override
            public void run() {
                untagPlayer(p, true);
            }
        }, tagTime * 20);

        tagged.put(p, id);
    }

    private void untagPlayer(Player p, boolean messagePlayer) {

        if (!tagged.containsKey(p)) return;

        hms.getServer().getScheduler().cancelTask(tagged.get(p));
        if (messagePlayer) {
            p.sendMessage(lang.get("untagged"));
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1, 2f);
        }
        tagged.remove(p);

    }

    @Override
    public void reloadConfig() {

        broadcastLog = hms.getConfig().getBoolean("combatLog.broadcastLog", true);
        tagTime = hms.getConfig().getInt("combatLog.tagTime", 30);

        lang.clear();
        lang.put("tagged", Language.getMessagePlaceholderReplace("modCombatLogTagged", true, "%PREFIX%", "PvP", "%TIME%", "" + tagTime));
        lang.put("untagged", Language.getMessagePlaceholderReplace("modCombatLogUntagged", true, "%PREFIX%", "PvP"));
        lang.put("loggedOut", Language.getMessagePlaceholderReplace("modCombatLogLoggedOut", true, "%PREFIX%", "PvP"));
        lang.put("noCommand", Language.getMessagePlaceholderReplace("modCombatLogNoCommand", true, "%PREFIX%", "PvP"));
        lang.put("noEnderpearl", Language.getMessagePlaceholderReplace("modCombatLogNoEnderpearl", true, "%PREFIX%", "PvP"));

    }
}
