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
    private String untaggedMessage;

    public ModCombatLog() {
        reloadConfig();
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onDeath(PlayerDeathEvent e) {
        if (tagged.containsKey(e.getEntity().getPlayer())) {
            untagPlayer(e.getEntity().getPlayer());
        }
        if (tagged.containsKey(e.getEntity().getKiller())) {
            untagPlayer(e.getEntity().getKiller());
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onLogout(PlayerQuitEvent e) {
        if (tagged.containsKey(e.getPlayer())) {

            //TODO Remove tag, kill if necessary
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {

        //TODO block command during fight
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {

        //TODO tag players / retag

    }

    @EventHandler(ignoreCancelled = true)
    public void onPotion(PotionSplashEvent e) {
        //TODO tag players / retag
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnderpearl(PlayerInteractEvent e) {
        //TODO disable during tag
    }

    private void tagPlayer(final Player p) {

        hms.getServer().getScheduler().scheduleSyncDelayedTask(hms, new Runnable() {
            @Override
            public void run() {
                untagPlayer(p);
            }
        }, tagTime);
    }

    private void untagPlayer(Player p) {

        hms.getServer().getScheduler().cancelTask(tagged.get(p));


    }

    @Override
    public void reloadConfig() {

        broadcastLog = hms.getConfig().getBoolean("combatLog.broadcastLog", true);
        tagTime = hms.getConfig().getInt("combatLog.tagTime", 30);

        lang.clear();
        lang.put("tagged", Language.getMessagePlaceholderReplace("modCombatLogTagged", true, "%PREFIX%", "PvP", "%TIME%", "" + tagTime));
        lang.put("untagged", Language.getMessagePlaceholderReplace("modCombatLogUntagged", true, "%PREFIX%", "PvP"));
        lang.put("loggedOut", Language.getMessagePlaceholderReplace("modCombatLogLoggedOut", true, "%PREFIX%", "PvP"));
        lang.put("noEommmand", Language.getMessagePlaceholderReplace("modCombatLogNoCommand", true, "%PREFIX%", "PvP"));
        lang.put("noEnderpearl", Language.getMessagePlaceholderReplace("modCombatLogNoEnderpearl", true, "%PREFIX%", "PvP"));

    }
}
