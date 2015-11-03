package de.halfminer.hmduel;

import de.halfminer.hmduel.module.ArenaQueue;
import de.halfminer.hmduel.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;

@SuppressWarnings("unused")
class Listeners implements Listener {

    private final HalfminerDuel hmd = HalfminerDuel.getInstance();
    private final ArenaQueue aq = hmd.getArenaQueue();

    @EventHandler(ignoreCancelled = true)
    public void disableEatDecay(FoodLevelChangeEvent e) {
        if (hmd.getConfig().getBoolean("noHungerLossInDuel") && e.getEntity() instanceof Player && aq.isInDuel((Player) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void disableInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player && aq.isInDuel((Player) e.getWhoClicked())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void disableItemDrop(PlayerDropItemEvent e) {
        if (aq.isInDuel(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void disableItemPickup(PlayerPickupItemEvent e) {
        if (aq.isInDuel(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void disableMovementDuringCountdown(PlayerMoveEvent e) {
        if (aq.isInDuel(e.getPlayer()) && e.getPlayer().getWalkSpeed() == 1.0E-4F) {
            e.getPlayer().teleport(e.getFrom());
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void enablePvPEvenIfBlocked(EntityDamageByEntityEvent e) {
        if (e.isCancelled() && e.getEntity() instanceof Player) { //make sure factions can fight
            Player gotHit = (Player) e.getEntity();
            if (aq.isInDuel(gotHit)) e.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void kickFromQueueOnPvP(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) { //kick from queue
            Player damager = (Player) e.getDamager();
            Player gotHit = (Player) e.getEntity();
            if (aq.isInQueue(damager)) aq.removeFromQueue(damager);
            if (aq.isInQueue(gotHit)) aq.removeFromQueue(gotHit);
        }
    }

    @EventHandler
    public void leaveRemoveQueueEndDuel(PlayerQuitEvent e) {
        Player didQuit = e.getPlayer();
        if (aq.isInQueue(didQuit)) {
            aq.removeFromQueue(didQuit);
        } else if (aq.isInDuel(didQuit)) {
            aq.gameHasFinished(didQuit, true);
        }
    }

    @EventHandler
    public void onChatSelectArena(AsyncPlayerChatEvent e) {
        if (aq.isSelectingArena(e.getPlayer())) {

            e.setCancelled(true);

            final Player player = e.getPlayer();
            final String message = e.getMessage();
            Bukkit.getScheduler().scheduleSyncDelayedTask(HalfminerDuel.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if (aq.isSelectingArena(player)) //It may happen that between event fire and task execution the partner leaves the battle, redo select check
                        aq.arenaWasSelected(player, message);
                }
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void disableCommandDuringFight(PlayerCommandPreprocessEvent e) {
        if (aq.isInDuel(e.getPlayer()) && !e.getPlayer().hasPermission("hmd.admin")) {
            Util.sendMessage(e.getPlayer(), "inGame");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeathEndFight(PlayerDeathEvent e) {
        Player died = e.getEntity().getPlayer();
        if (aq.isInQueue(died)) aq.removeFromQueue(died);
        if (aq.isInDuel(died)) aq.gameHasFinished(died, true);
    }

}
