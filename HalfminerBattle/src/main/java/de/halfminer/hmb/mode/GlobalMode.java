package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
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

/**
 * Global game mode, functionality shared by all other {@link de.halfminer.hmb.mode.abs.GameMode}
 */
@SuppressWarnings("unused")
public class GlobalMode extends AbstractMode {

    private boolean noHungerLossInBattle;
    private int queueCooldownSeconds;
    private double teleportSpawnDistance;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPvPUncancel(EntityDamageByEntityEvent e) {
        // Allow Faction members to fight
        if (e.isCancelled()
                && e.getEntity() instanceof Player
                && pm.isInBattle((Player) e.getEntity()))
            e.setCancelled(false);
    }

    @EventHandler(ignoreCancelled = true)
    public void disableCommandDuringFight(PlayerCommandPreprocessEvent e) {

        if (pm.isInBattle(e.getPlayer()) && !e.getPlayer().hasPermission("hmb.admin")) {
            MessageBuilder.create(hmb, "modeGlobalInGame", HalfminerBattle.PREFIX).sendMessage(e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void eatDecayDisable(FoodLevelChangeEvent e) {

        if (noHungerLossInBattle && e.getEntity() instanceof Player
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
        if (!pm.isInBattle(e.getPlayer())
                && !e.getPlayer().hasPermission("hmb.global.bypass.teleportintoarena")
                && am.isArenaSpawn(e.getTo())) {
            MessageBuilder.create(hmb, "modeGlobalTeleportIntoArenaDenied", HalfminerBattle.PREFIX).sendMessage(e.getPlayer());
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
            MessageBuilder.create(hmb, success ? "adminSettingsReloaded" : "adminSettingsReloadedError", HalfminerBattle.PREFIX)
                    .sendMessage(sender);
        } else {

            // disregard if called via global custom gamemode /hmb glo(balmode)
            if (args.length < 3 || args[0].toLowerCase().startsWith("glo")) {
                sendUsageInformation(sender);
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
                    sendStatusMessage(sender, successCreate ? "adminCreate" : "adminCreateFailed", args[2], type);
                    break;
                case "remove":
                    boolean successRemove = am.delArena(type, args[2]);
                    sendStatusMessage(sender, successRemove ? "adminRemove" : "adminArenaDoesntExist", args[2], type);
                    break;
                case "setspawn":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    int spawnNumber = Integer.MAX_VALUE;
                    if (args.length > 3) spawnNumber = getNumberFromString(args[3]) - 1;
                    boolean successSetSpawn = am.setSpawn(type, args[2], player.getLocation(), spawnNumber);
                    sendStatusMessage(sender, successSetSpawn ? "adminSetSpawn" : "adminArenaDoesntExist", args[2], type);
                    break;
                case "removespawn":
                    int spawnNumberToRemove = 0;
                    if (args.length > 3) spawnNumberToRemove = getNumberFromString(args[3]) - 1;
                    boolean successClear = am.removeSpawn(type, args[2], spawnNumberToRemove);
                    sendStatusMessage(sender, successClear ? "adminClearSpawns" : "adminArenaDoesntExist", args[2], type);
                    break;
                case "setkit":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    boolean successKit = am.setKit(type, args[2], player.getInventory());
                    sendStatusMessage(sender, successKit ? "adminSetKit" : "adminArenaDoesntExist", args[2], type);
                    break;
                default:
                    sendUsageInformation(sender);
            }
        }
        return true;
    }

    private void sendUsageInformation(CommandSender sendTo) {
        MessageBuilder.create(hmb, "adminCommandUsage", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%VERSION%", hmb.getDescription().getVersion())
                .sendMessage(sendTo);
    }

    private void sendStatusMessage(CommandSender sendTo, String messageKey, String arenaName, GameModeType mode) {
        MessageBuilder.create(hmb, messageKey, HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%ARENA%", arenaName)
                .addPlaceholderReplace("%MODE%", Utils.makeStringFriendly(mode.toString()))
                .sendMessage(sendTo);
    }

    private int getNumberFromString(String toParse) {
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private void sendNotAPlayerMessage(CommandSender sendTo) {
        MessageBuilder.create(hmb, "notAPlayer", HalfminerBattle.PREFIX).sendMessage(sendTo);
    }

    public int getQueueCooldownSeconds() {
        return queueCooldownSeconds;
    }

    public double getTeleportSpawnDistance() {
        return teleportSpawnDistance;
    }

    @Override
    public void onPluginDisable() {}

    @Override
    public void onConfigReload() {
        noHungerLossInBattle = hmb.getConfig().getBoolean("gameMode.global.noHungerLoss", true);
        queueCooldownSeconds = hmb.getConfig().getInt("gameMode.global.queueCooldownTimeSeconds", 15);
        teleportSpawnDistance = hmb.getConfig().getDouble("gameMode.global.teleportSpawnDistance", 10.0d);
    }
}
