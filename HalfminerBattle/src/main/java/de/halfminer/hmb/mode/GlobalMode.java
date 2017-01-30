package de.halfminer.hmb.mode;

import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hmb.util.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@SuppressWarnings("unused")
public class GlobalMode extends AbstractMode {

    private boolean noHungerLossInDuel;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvPUncancel(EntityDamageByEntityEvent e) {

        // Allow Faction members to duel
        if (e.isCancelled()
                && e.getEntity() instanceof Player
                && pm.isInBattle((Player) e.getEntity()))
            e.setCancelled(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void disableCommandDuringFight(PlayerCommandPreprocessEvent e) {
        if (pm.isInBattle(e.getPlayer()) && !e.getPlayer().hasPermission("hmb.admin")) {
            Util.sendMessage(e.getPlayer(), "inGame");
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void eatDecayDisable(FoodLevelChangeEvent e) {

        if (noHungerLossInDuel && e.getEntity() instanceof Player
                && pm.isInBattle((Player) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void inventoryClickDisableInFight(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player && pm.isInBattle((Player) e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void itemDropDisable(PlayerDropItemEvent e) {
        if (pm.isInBattle(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void itemPickupDisable(PlayerPickupItemEvent e) {
        if (pm.isInBattle(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void teleportDisable(PlayerTeleportEvent e) {
        if (!pm.isInBattle(e.getPlayer()) && am.isArenaSpawn(e.getTo())) {
            Util.sendMessage(e.getPlayer(), "teleportIntoArenaDenied");
            e.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {
        //TODO move arena commands
        return false;
    }

    @Override
    public void onPluginDisable() {}

    @Override
    public void onConfigReload() {
        noHungerLossInDuel = hmb.getConfig().getBoolean("noHungerLossInDuel", true);
    }
}
