package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
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
            MessageBuilder.create(hmb, "inGame", HalfminerBattle.PREFIX).sendMessage(e.getPlayer());
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
            MessageBuilder.create(hmb, "teleportIntoArenaDenied", HalfminerBattle.PREFIX).sendMessage(e.getPlayer());
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
        if (args[0].equalsIgnoreCase("reload")) {
            boolean success = hmb.saveAndReloadConfig();
            MessageBuilder.create(hmb, success ? "adminSettingsReloaded" : "adminSettingsReloadedError")
                    .sendMessage(sender);
            return true;
        }
        return false;
    }

    @Override
    public void onPluginDisable() {}

    @Override
    public void onConfigReload() {
        noHungerLossInDuel = hmb.getConfig().getBoolean("noHungerLossInDuel", true);
    }
}