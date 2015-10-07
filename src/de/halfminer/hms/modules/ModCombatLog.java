package de.halfminer.hms.modules;

import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.util.Language;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
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
        if (tagged.containsKey(e.getEntity().getPlayer())) {
            untagPlayer(e.getEntity().getPlayer(), false);
        }
        if (tagged.containsKey(e.getEntity().getKiller())) {
            untagPlayer(e.getEntity().getKiller(), true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onLogout(PlayerQuitEvent e) {
        if (tagged.containsKey(e.getPlayer())) {
            untagPlayer(e.getPlayer(), false);

            EntityDamageByEntityEvent e2 = (EntityDamageByEntityEvent) e.getPlayer().getLastDamageCause();
            if (e2.getDamager() instanceof Player) {
                untagPlayer((Player) e2.getDamager(), true);
            }
            if(broadcastLog)
                hms.getServer().broadcast(Language.placeholderReplace(lang.get("loggedOut"), "%PLAYER%", e.getPlayer().getName()), e.getPlayer().getName());
            e.getPlayer().setHealth(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onPvP(EntityDamageByEntityEvent e) {

        //TODO tag players / retag

    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onPotion(PotionSplashEvent e) {
        //TODO tag players / retag
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if(tagged.containsKey(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(lang.get("noCommand"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onEnderpearl(PlayerInteractEvent e) {
        if(tagged.containsKey(e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(lang.get("noEnderpearl"));
        }
    }

    private void tagPlayer(final Player p) {

        if(tagged.containsKey(p)) hms.getServer().getScheduler().cancelTask(tagged.get(p));
        else p.sendMessage(lang.get("tagged"));

        int id = hms.getServer().getScheduler().scheduleSyncDelayedTask(hms, new Runnable() {
            @Override
            public void run() {
                untagPlayer(p, true);
            }
        }, tagTime);

        tagged.put(p, id);
    }

    private void untagPlayer(Player p, boolean messagePlayer) {

        hms.getServer().getScheduler().cancelTask(tagged.get(p));
        if (messagePlayer) p.sendMessage(lang.get("untagged"));
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
        lang.put("noCommmand", Language.getMessagePlaceholderReplace("modCombatLogNoCommand", true, "%PREFIX%", "PvP"));
        lang.put("noEnderpearl", Language.getMessagePlaceholderReplace("modCombatLogNoEnderpearl", true, "%PREFIX%", "PvP"));

    }
}
