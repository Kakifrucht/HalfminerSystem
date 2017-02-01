package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.enums.GameModeType;
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

        if (args[0].equalsIgnoreCase("reload")) {

            boolean success = hmb.saveAndReloadConfig();
            MessageBuilder.create(hmb, success ? "adminSettingsReloaded" : "adminSettingsReloadedError")
                    .sendMessage(sender);
        } else {

            if (args.length < 3) {
                MessageBuilder.create(hmb, "adminHelp", HalfminerBattle.PREFIX).sendMessage(sender);
                return true;
            }

            GameModeType type = GameModeType.getGameMode(args[1]);
            if (type == null) {
                MessageBuilder.create(hmb, "adminUnknownGamemode", HalfminerBattle.PREFIX).sendMessage(sender);
                return true;
            }

            boolean isPlayer = sender instanceof Player;
            Player player = isPlayer ? (Player) sender : null;

            String arg = args[0].toLowerCase();
            switch (arg) {
                case "create":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    boolean successCreate = am.addArena(type, args[2], player.getLocation());
                    sendStatusMessage(sender, successCreate ? "adminCreate" : "adminCreateFailed", args[2]);
                    break;
                case "remove":
                    boolean successRemove = am.delArena(type, args[2]);
                    sendStatusMessage(sender, successRemove ? "adminRemove" : "adminArenaDoesntExist", args[2]);
                    break;
                case "setspawn":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    int spawnNumber = Integer.MAX_VALUE;
                    if (args.length > 3) {
                        try {
                            spawnNumber = Integer.parseInt(args[3]);
                        } catch (NumberFormatException ignored) {}
                    }
                    boolean successSetSpawn = am.setSpawn(type, args[2], player.getLocation(), spawnNumber);
                    sendStatusMessage(sender, successSetSpawn ? "adminSetSpawn" : "adminArenaDoesntExist", args[2]);
                    break;
                case "clearspawns":
                    boolean successClear = am.clearSpawns(type, args[2]);
                    sendStatusMessage(sender, successClear ? "adminClearSpawns" : "adminArenaDoesntExist", args[2]);
                    break;
                case "setkit":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    boolean successKit = am.setKit(type, args[2], player.getInventory());
                    sendStatusMessage(sender, successKit ? "adminSetKit" : "adminArenaDoesntExist", args[2]);
                    break;
                default:
                    MessageBuilder.create(hmb, "adminHelp", HalfminerBattle.PREFIX).sendMessage(sender);
            }
        }
        return true;
    }

    private void sendStatusMessage(CommandSender sender, String messageKey, String arenaName) {
        MessageBuilder.create(hmb, messageKey, HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%ARENA%", arenaName)
                .sendMessage(sender);
    }

    private void sendNotAPlayerMessage(CommandSender sender) {
        MessageBuilder.create(hmb, "notAPlayer", HalfminerBattle.PREFIX).sendMessage(sender);
    }

    @Override
    public void onPluginDisable() {}

    @Override
    public void onConfigReload() {
        noHungerLossInDuel = hmb.getConfig().getBoolean("noHungerLossInDuel", true);
    }
}
