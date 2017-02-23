package de.halfminer.hmb.mode;

import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.enums.BattleModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hmb.mode.abs.BattleMode;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global game mode, functionality shared by all other {@link BattleMode}
 */
@SuppressWarnings("unused")
public class GlobalMode extends AbstractMode {

    private boolean noHungerLossInBattle;
    private Set<String> nonBlockedCommands;
    private int queueCooldownSeconds;
    private boolean saveInventoryToDisk;
    private BukkitTask cleanupTask;
    private double teleportSpawnDistance;

    public GlobalMode() {
        super(BattleModeType.GLOBAL);
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {

        // disregard if called via global custom battle mode /hmb glo(balmode)
        if (args.length == 0 || args[0].toLowerCase().startsWith("glo")) {
            sendUsageInformation(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {

            boolean success = hmb.saveAndReloadConfig();
            MessageBuilder.create(success ? "adminSettingsReloaded" : "adminSettingsReloadedError", hmb)
                    .sendMessage(sender);
        } else if (args[0].equalsIgnoreCase("openinventory")) {

            if (args.length < 2) {
                sendUsageInformation(sender);
                return true;
            }

            String fileName = args[1];
            if (!fileName.endsWith(".yml")) {
                fileName += ".yml";
            }

            File fileToOpen = new File(hmb.getDataFolder() + File.separator + "inventories", fileName);
            if (fileToOpen.exists()) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fileToOpen);
                List<?> items = yaml.getList("inventory");

                if (items != null) {

                    Player toRestore = null;
                    if (args.length > 2 && args[2].equals("-r")) {
                        String uuidString = yaml.getString("uuid");
                        toRestore = hmb.getServer().getPlayer(UUID.fromString(uuidString));
                        if (toRestore == null || !toRestore.isOnline()) {
                            MessageBuilder.create("playerNotOnline", "Battle")
                                    .sendMessage(sender);
                            return true;
                        }
                        if (pm.isInBattle(type, toRestore)) {
                            MessageBuilder.create("adminOpenInventoryRestoredError", hmb)
                                    .addPlaceholderReplace("%PLAYER%", toRestore.getName())
                                    .sendMessage(sender);
                            return true;
                        }
                    }

                    ItemStack[] contents = new ItemStack[items.size()];
                    for (int i = 0; i < 45 && i < items.size(); i++) {
                        Object item = items.get(i);
                        if (item instanceof ItemStack) {
                            contents[i] = (ItemStack) item;
                        }
                    }

                    if (toRestore != null) {
                        toRestore.getInventory().setContents(contents);
                        MessageBuilder.create("adminOpenInventoryRestored", hmb)
                                .addPlaceholderReplace("%PLAYER%", toRestore.getName())
                                .sendMessage(sender);
                    } else {

                        if (!(sender instanceof Player)) {
                            sendNotAPlayerMessage(sender);
                            return true;
                        }

                        Player player = (Player) sender;

                        int modulo = contents.length % 9;
                        int size = modulo == 0 ? contents.length : contents.length + (9 - modulo);
                        Inventory toOpen = hmb.getServer().createInventory(player, size);
                        toOpen.setContents(contents);
                        player.openInventory(toOpen);
                    }
                } else {
                    MessageBuilder.create("adminOpenInventoryInvalid", hmb).sendMessage(sender);
                }
            } else {
                MessageBuilder.create("adminOpenInventoryUnknownFile", hmb).sendMessage(sender);
            }

        } else {

            if (args.length < 3) {
                sendUsageInformation(sender);
                return true;
            }

            BattleModeType type = BattleModeType.getBattleMode(args[1]);
            if (type == null || type.equals(BattleModeType.GLOBAL)) {
                MessageBuilder.create("adminUnknownBattleMode", hmb).sendMessage(sender);
                return true;
            }

            boolean isPlayer = sender instanceof Player;
            Player player = isPlayer ? (Player) sender : null;

            String arg = args[0].toLowerCase();
            boolean success = false;
            switch (arg) {
                case "create":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    success = am.addArena(type, args[2], Collections.singletonList(player.getLocation()));
                    sendStatusMessage(sender, success ? "adminCreate" : "adminCreateFailed", args[2], type);
                    break;
                case "remove":
                    success = am.delArena(type, args[2]);
                    sendStatusMessage(sender, success ? "adminRemove" : "adminArenaDoesntExist", args[2], type);
                    break;
                case "setspawn":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    int spawnNumber = Integer.MAX_VALUE;
                    if (args.length > 3) spawnNumber = getNumberFromString(args[3]) - 1;
                    success = am.setSpawn(type, args[2], player.getLocation(), spawnNumber);
                    sendStatusMessage(sender, success ? "adminSetSpawn" : "adminArenaDoesntExist", args[2], type);
                    break;
                case "removespawn":
                    int spawnNumberToRemove = 0;
                    if (args.length > 3) spawnNumberToRemove = getNumberFromString(args[3]) - 1;
                    success = am.removeSpawn(type, args[2], spawnNumberToRemove);
                    sendStatusMessage(sender, success ? "adminClearSpawns" : "adminArenaDoesntExist", args[2], type);
                    break;
                case "setkit":
                    if (!isPlayer) {
                        sendNotAPlayerMessage(sender);
                        return true;
                    }
                    success = am.setKit(type, args[2], player.getInventory());
                    sendStatusMessage(sender, success ? "adminSetKit" : "adminArenaDoesntExist", args[2], type);
                    break;
                case "forceend":
                    Arena arenaToEnd = am.getArena(type, args[2]);
                    boolean endWasForced = false;
                    if (arenaToEnd != null) {
                        endWasForced = arenaToEnd.forceGameEnd();
                    }
                    sendStatusMessage(sender, endWasForced ? "adminForcedEnd" : "adminForcedEndError", args[2], type);
                    break;
                default:
                    sendUsageInformation(sender);
            }
            if (success) am.saveData();
        }
        return true;
    }

    private void sendUsageInformation(CommandSender sendTo) {
        MessageBuilder.create("adminCommandUsage", hmb)
                .addPlaceholderReplace("%VERSION%", hmb.getDescription().getVersion())
                .sendMessage(sendTo);
    }

    private void sendStatusMessage(CommandSender sendTo, String messageKey, String arenaName, BattleModeType mode) {
        MessageBuilder.create(messageKey, hmb)
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
        MessageBuilder.create("notAPlayer", "Battle").sendMessage(sendTo);
    }

    @Override
    public void onConfigReload() {

        noHungerLossInBattle = hmb.getConfig().getBoolean("battleMode.global.noHungerLoss", true);

        nonBlockedCommands = hmb.getConfig().getStringList("battleMode.global.nonBlockedCommands")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        queueCooldownSeconds = hmb.getConfig().getInt("battleMode.global.queueCooldownTimeSeconds", 15);

        saveInventoryToDisk = hmb.getConfig().getBoolean("battleMode.global.saveInventoryToDisk", true);

        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        // only run cleanup if saveinventory is enabled and autoclean is set higher than 0
        int cleanupAfter = hmb.getConfig().getInt("battleMode.global.saveInventoryToDiskCleanupAfterHours", 24);
        if (saveInventoryToDisk && cleanupAfter > 0) {

            cleanupTask = hmb.getServer().getScheduler().runTaskTimerAsynchronously(hmb, () -> {

                File[] files = new File(hmb.getDataFolder(), "inventories").listFiles();
                if (files == null) return;

                long removeIfBefore = (System.currentTimeMillis() / 1000) - cleanupAfter * 60 * 60;
                for (File file : files) {
                    String name = file.getName();
                    long timestamp = file.lastModified() / 1000;
                    if (timestamp > 0L) {
                        if (removeIfBefore > timestamp && !file.delete()) {
                            hmb.getLogger().warning("Could not delete file " + name);
                        }
                    } else {
                        hmb.getLogger().warning("Invalid file, could not delete " + name);
                    }
                }
            }, 0L, 72000L);
        }

        teleportSpawnDistance = hmb.getConfig().getDouble("battleMode.global.teleportSpawnDistance", 10.0d);
    }

    public int getQueueCooldownSeconds() {
        return queueCooldownSeconds;
    }

    public boolean isSaveInventoryToDisk() {
        return saveInventoryToDisk;
    }

    public double getTeleportSpawnDistance() {
        return teleportSpawnDistance;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathKeepInventory(PlayerDeathEvent e) {
        if (pm.isInBattle(type, e.getEntity())) {
            e.setKeepInventory(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPvPUncancel(EntityDamageByEntityEvent e) {
        // Allow Faction members to fight
        if (e.isCancelled()
                && e.getEntity() instanceof Player
                && pm.isInBattle(type, (Player) e.getEntity()))
            e.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void disableCommandDuringFight(PlayerCommandPreprocessEvent e) {

        if (pm.isInBattle(type, e.getPlayer())
                && !e.getPlayer().hasPermission("hmb.mode.global.bypass.commands")
                && !nonBlockedCommands.contains(e.getMessage().substring(1).toLowerCase())
                && (!pm.isInBattle(BattleModeType.FFA, e.getPlayer()) || !e.getMessage().startsWith("/ffa"))) {

            MessageBuilder.create("modeGlobalNoCommandInGame", hmb).sendMessage(e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void eatDecayDisable(FoodLevelChangeEvent e) {

        if (noHungerLossInBattle && e.getEntity() instanceof Player
                && pm.isInBattle(type, (Player) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void itemDropRemove(PlayerDropItemEvent e) {
        if (pm.isInBattle(type, e.getPlayer())) {
            e.getItemDrop().remove();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void itemPickupDisable(PlayerPickupItemEvent e) {
        e.setCancelled(pm.isInBattle(type, e.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void teleportDisable(PlayerTeleportEvent e) {
        if (!pm.isInBattle(type, e.getPlayer())
                && !e.getPlayer().hasPermission("hmb.global.bypass.teleportintoarena")
                && am.isArenaSpawn(e.getTo())) {
            MessageBuilder.create("modeGlobalTeleportIntoArenaDenied", hmb).sendMessage(e.getPlayer());
            e.setCancelled(true);
        }
    }
}
