package de.halfminer.hmb.mode;

import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hmb.mode.abs.BattleMode;
import de.halfminer.hmb.mode.abs.BattleModeType;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Utils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Global game mode, functionality shared by all other {@link BattleMode}
 */
@SuppressWarnings("unused")
public class GlobalMode extends AbstractMode {

    // config settings
    private boolean noHungerLossInBattle;
    private Set<String> nonBlockedCommands;
    private int queueCooldownSeconds;
    private boolean saveInventoryToDisk;
    private BukkitTask cleanupTask;
    private double teleportSpawnDistance;

    /**
     * Allow friendly fire via potions in arenas
     */
    private boolean uncancelNextPotionSplashEvent = false;

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
            Message.create(success ? "adminSettingsReloaded" : "adminSettingsReloadedError", hmb)
                    .send(sender);
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
                        toRestore = server.getPlayer(UUID.fromString(uuidString));
                        if (toRestore == null || !toRestore.isOnline()) {
                            Message.create("playerNotOnline", "Battle")
                                    .send(sender);
                            return true;
                        }
                        if (pm.isInBattle(type, toRestore)) {
                            Message.create("adminOpenInventoryRestoredError", hmb)
                                    .addPlaceholder("%PLAYER%", toRestore.getName())
                                    .send(sender);
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
                        Message.create("adminOpenInventoryRestored", hmb)
                                .addPlaceholder("%PLAYER%", toRestore.getName())
                                .send(sender);
                    }

                    if (!(sender instanceof Player)) {
                        if (toRestore == null) {
                            sendNotAPlayerMessage(sender);
                        }
                        return true;
                    }

                    Player player = (Player) sender;

                    int modulo = contents.length % 9;
                    int size = modulo == 0 ? contents.length : contents.length + (9 - modulo);
                    Inventory toOpen = server.createInventory(player, size);
                    toOpen.setContents(contents);
                    player.openInventory(toOpen);

                } else {
                    Message.create("adminOpenInventoryInvalid", hmb).send(sender);
                }
            } else {
                Message.create("adminOpenInventoryUnknownFile", hmb).send(sender);
            }

        } else {

            if (args.length < 3) {
                sendUsageInformation(sender);
                return true;
            }

            BattleModeType type = BattleModeType.getBattleMode(args[1]);
            if (type == null || type.equals(BattleModeType.GLOBAL)) {
                Message.create("adminUnknownBattleMode", hmb).send(sender);
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
                    boolean setEmptyKit = false;
                    ItemStack[] kit;
                    if (args.length > 3 && args[3].equals("-e")) {
                        kit = new ItemStack[41];
                        setEmptyKit = true;
                    } else {
                        if (!isPlayer) {
                            sendNotAPlayerMessage(sender);
                            return true;
                        }
                        kit = player.getInventory().getContents();
                    }

                    success = am.setKit(type, args[2], kit);
                    sendStatusMessage(sender, success ?
                            setEmptyKit ? "adminSetEmptyKit" : "adminSetKit"
                            : "adminArenaDoesntExist", args[2], type);
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
        Message.create("adminCommandUsage", hmb)
                .addPlaceholder("%VERSION%", hmb.getDescription().getVersion())
                .addPlaceholder("%SYSTEMVERSION%", hms.getDescription().getVersion())
                .send(sendTo);
    }

    private void sendStatusMessage(CommandSender sendTo, String messageKey, String arenaName, BattleModeType mode) {
        Message.create(messageKey, hmb)
                .addPlaceholder("%ARENA%", arenaName)
                .addPlaceholder("%MODE%", Utils.makeStringFriendly(mode.toString()))
                .send(sendTo);
    }

    private int getNumberFromString(String toParse) {
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private void sendNotAPlayerMessage(CommandSender sendTo) {
        Message.create("notAPlayer", "Battle").send(sendTo);
    }

    @Override
    public void loadConfig() {

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

            cleanupTask = scheduler.runTaskTimerAsynchronously(hmb, () -> {

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

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDisconnectSetDisconnected(PlayerQuitEvent e) {
        if (pm.isInBattle(type, e.getPlayer())) {
            pm.setHasDisconnected(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPvPUncancel(EntityDamageByEntityEvent e) {
        // Allow Faction members to do damage
        uncancelDamageEvent(e, e.getDamager(), e.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPvPCombustUncancel(EntityCombustByEntityEvent e) {
        uncancelDamageEvent(e, e.getCombuster(), e.getEntity());
    }

    private void uncancelDamageEvent(Cancellable event, Entity attackerEntity, Entity victimEntity) {
        if (event.isCancelled()
                && victimEntity instanceof Player
                && pm.isInBattle(type, (Player) victimEntity)
                && !victimEntity.equals(Utils.getPlayerSourceFromEntity(attackerEntity))) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSplashUncancelStore(PotionSplashEvent e) {
        // to prevent factions/others from removing players from affectedEntities collection do cancel/uncancelling
        if (e.getEntity().getShooter() instanceof Player) {
            Player shooter = (Player) e.getEntity().getShooter();
            if (pm.isInBattle(type, shooter)) {
                uncancelNextPotionSplashEvent = true;
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSplashUncancelRecover(PotionSplashEvent e) {
        if (e.isCancelled() && uncancelNextPotionSplashEvent) {
            e.setCancelled(false);
            uncancelNextPotionSplashEvent = false;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void disableCommandDuringFight(PlayerCommandPreprocessEvent e) {

        if (pm.isInBattle(type, e.getPlayer())
                && !e.getPlayer().hasPermission("hmb.mode.global.bypass.commands")
                && !nonBlockedCommands.contains(e.getMessage().substring(1).toLowerCase())
                && (!pm.isInBattle(BattleModeType.FFA, e.getPlayer()) || !e.getMessage().startsWith("/ffa"))) {

            Message.create("modeGlobalNoCommandInGame", hmb).send(e.getPlayer());
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void itemDropRemove(PlayerDropItemEvent e) {

        Player p = e.getPlayer();
        if (pm.isInBattle(type, p)) {
            if (pm.isUsingOwnEquipment(p)) {
                e.setCancelled(true);
            } else {
                e.getItemDrop().remove();
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void itemPickupDisable(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) {
            e.setCancelled(pm.isInBattle(type, (Player) e.getEntity()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void arrowPickupDisable(PlayerPickupArrowEvent e) {
        e.setCancelled(pm.isInBattle(type, e.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void teleportDisable(PlayerTeleportEvent e) {
        if (!pm.isInBattle(type, e.getPlayer())
                && !e.getPlayer().hasPermission("hmb.mode.global.bypass.teleportintoarena")
                && am.isArenaSpawn(e.getTo())) {
            Message.create("modeGlobalTeleportIntoArenaDenied", hmb).send(e.getPlayer());
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void tameableTeleportDisable(EntityTeleportEvent e) {
        if (e.getEntity() instanceof Tameable) {
            Tameable entity = (Tameable) e.getEntity();
            if (entity.getOwner() instanceof Player) {
                e.setCancelled(pm.isInBattle(type, (Player) entity.getOwner()));
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void craftDeny(CraftItemEvent e) {

        if (!(e.getWhoClicked() instanceof Player)) return;
        e.setCancelled(pm.isInBattle(type, (Player) e.getWhoClicked()));
    }

    @EventHandler(ignoreCancelled = true)
    public void inventoryOpenDeny(InventoryOpenEvent e) {

        if (!(e.getPlayer() instanceof Player)) return;
        e.setCancelled(pm.isInBattle(type, (Player) e.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void inventoryCloseRestoreHeldItemAndCheckAll(InventoryCloseEvent e) {

        if (!(e.getPlayer() instanceof Player))
            return;

        Player player = (Player) e.getPlayer();
        if (pm.isInBattle(type, player)) {
            Inventory inv = player.getInventory();

            ItemStack cursor = e.getView().getCursor();
            if (cursor != null && !cursor.getType().equals(Material.AIR)) {
                e.getView().setCursor(null);
                HashMap<Integer, ItemStack> returned = inv.addItem(cursor);
                if (returned.size() != 0) {
                    player.getWorld().dropItem(player.getLocation(), cursor);
                }
            }

            for (int i = 0; i < inv.getContents().length; i++) {
                ItemStack current = inv.getContents()[i];
                if (pm.checkAndStoreItemStack(player, current)) {
                    inv.setItem(i, null);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractCheckItem(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getAction().equals(Action.PHYSICAL) && pm.isInBattle(type, p)) {
            if (pm.checkAndStoreItemStack(p, e.getItem())) {

                e.setCancelled(true);
                if (e.getHand().equals(EquipmentSlot.HAND)) {
                    p.getInventory().setItemInMainHand(null);
                } else {
                    p.getInventory().setItemInOffHand(null);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamageCheckItem(EntityDamageByEntityEvent e) {
        Player damager = Utils.getPlayerSourceFromEntity(e.getDamager());
        if (damager != null
                && pm.isInBattle(type, damager)) {
            ItemStack hand = damager.getInventory().getItemInMainHand();
            if (pm.checkAndStoreItemStack(damager, hand)) {
                damager.getInventory().setItemInMainHand(null);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void denyBedEnter(PlayerBedEnterEvent e) {
        e.setCancelled(pm.isInBattle(type, e.getPlayer()));
    }
}
