package de.halfminer.hms.modules;

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

    private final Map<Player, Long> loggedPlayers = new HashMap<>();

    public ModCombatLog() {
        reloadConfig();
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onDeath(PlayerDeathEvent e) {
        if(loggedPlayers.containsKey(e.getEntity().getPlayer())) {

            //TODO remove tag(s)
        }
    }

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    public void onLogout(PlayerQuitEvent e) {
        if(loggedPlayers.containsKey(e.getPlayer())) {

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

    @Override
    public void reloadConfig() {

        //TODO load parameters
    }
}
