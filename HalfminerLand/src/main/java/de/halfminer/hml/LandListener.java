package de.halfminer.hml;

import de.halfminer.hml.land.Board;
import de.halfminer.hml.land.FlyBoard;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.manageable.Reloadable;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.Pair;
import de.halfminer.hms.util.StringArgumentSeparator;
import de.halfminer.hms.util.Utils;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;

import java.util.*;

/**
 * Listener class for the entire code base. Handles Bukkit events, like moving between chunks,
 */
public class LandListener extends LandClass implements Listener, Reloadable {

    private final Board board;
    private final WorldGuardHelper wgh;

    private final Map<Player, Chunk> lastKnownChunk;

    private Set<String> blockedCmds;
    private double landNotProtectedMessagePercentage;


    LandListener(Board board, WorldGuardHelper wgh) {
        this.board = board;
        this.wgh = wgh;

        this.lastKnownChunk = new HashMap<>();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        setLastKnownChunk(e.getPlayer());
        board.updatePlayerLocation(e.getPlayer(), null, e.getPlayer().getLocation().getChunk());

        // remove abandonment status if land is abandoned
        Set<Land> ownedLands = board.getLands(e.getPlayer());
        if (!ownedLands.isEmpty()) {

            int count = 0;
            for (Land ownedLand : ownedLands) {
                if (ownedLand.isAbandoned()) {
                    ownedLand.updateAbandonmentStatus();
                    count++;
                }
            }

            if (count > 0) {
                Message.create("listenerLandUnabandoned", hml)
                        .addPlaceholder("%COUNT%", count)
                        .addPlaceholder("%DAYSUNTILABANDONED%", hml.getConfig().getInt("landAbandonedAfterDays", 21))
                        .send(e.getPlayer());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {

        Player player = e.getPlayer();
        board.updatePlayerLocation(player, e.getPlayer().getLocation().getChunk(), null);

        FlyBoard flyBoard = board.getFlyBoard();
        if (flyBoard.isPlayerFlying(player)) {
            flyBoard.togglePlayerFlying(player);
        }

        lastKnownChunk.remove(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        onLocationChange(e.getPlayer(), e.getPlayer().getLocation(), e.getRespawnLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        onMove(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        onLocationChange(e.getPlayer(), e.getFrom(), e.getTo());
    }

    private void onLocationChange(Player player, Location from, Location to) {

        // ignore location changes after logout
        if (!lastKnownChunk.containsKey(player)) {
            return;
        }

        Chunk previousChunk = lastKnownChunk.get(player);
        Chunk newChunk = to.getChunk();
        if (!newChunk.equals(previousChunk)) {

            lastKnownChunk.put(player, newChunk);

            Pair<Land, Land> previousAndNewLandPair = board.updatePlayerLocation(player, previousChunk, newChunk);
            Land previousLand = previousAndNewLandPair.getLeft();
            Land newLand = previousAndNewLandPair.getRight();

            String pvpMessageInTitle = "";
            String spacer = "";
            String ownerMessageInTitle = "";

            boolean currentIsPvP = wgh.isPvPEnabled(to);
            boolean pvpStateChanged = wgh.isPvPEnabled(from) ^ currentIsPvP;

            if (pvpStateChanged) {
                pvpMessageInTitle = Message.returnMessage("listenerPvP" + (currentIsPvP ? "On" : "Off"), hml, false);
            }

            if (!newLand.hasOwner()) {

                boolean newLandIsFree = wgh.isLandFree(newLand);

                if (previousLand.hasOwner() || (newLandIsFree != wgh.isLandFree(previousLand))) {
                    String messageKey = "listenerOwner" + (newLandIsFree ? "Free" : "Protected");
                    ownerMessageInTitle = Message.returnMessage(messageKey, hml, false);
                }

            } else if (newLand.hasOwner() && hasDifferentOwner(newLand, previousLand)) {

                ownerMessageInTitle = Message.create("listenerOwnerOwned" + (newLand.isAbandoned() ? "Abandoned" : ""), hml)
                        .togglePrefix()
                        .addPlaceholder("%OWNER%", newLand.getOwnerName())
                        .returnMessage();
            }

            // add spacer if necessary
            if (pvpStateChanged && !ownerMessageInTitle.isEmpty()) {
                spacer = Message.returnMessage("listenerFormatSpacer", hml, false);
            }

            if (pvpStateChanged || !ownerMessageInTitle.isEmpty()) {

                hms.getTitlesHandler().sendTitle(player, Message.create("listenerFormatTitle", hml)
                        .togglePrefix()
                        .addPlaceholder("%PVPLINE%", pvpMessageInTitle)
                        .addPlaceholder("%SPACER%", spacer)
                        .addPlaceholder("%OWNERLINE%", ownerMessageInTitle)
                        .returnMessage(), 0, 40, 10);
            }

            // toggle flying if entering/leaving owned land
            board.getFlyBoard().updatePlayerAllowFlight(player, newLand);

            if (newLand.hasTitle()) {
                hms.getTitlesHandler().sendActionBar(player, newLand.getTitle());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {

        if (e.getPlayer().hasPermission("hml.bypass.cmd")) {
            return;
        }

        // check for blocked cmds on land
        Land currentLand = board.getLandAt(e.getPlayer());
        if (!currentLand.hasPermission(e.getPlayer())) {

            String command = e.getMessage().substring(1).toLowerCase();

            for (String blockedCmd : blockedCmds) {

                if (command.startsWith(blockedCmd)) {
                    e.setCancelled(true);
                    Message.create("listenerCmdBlocked", hml)
                            .addPlaceholder("%OWNER%", currentLand.getOwnerName())
                            .send(e.getPlayer());
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        boolean cancel = sendNoBreakPermissionMessage(e, e.getPlayer());
        e.setCancelled(cancel);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        boolean cancel = sendNoBreakPermissionMessage(e, e.getPlayer());
        e.setCancelled(cancel);
    }

    private boolean sendNoBreakPermissionMessage(BlockEvent e, Player player) {

        if (player.hasPermission("hml.bypass.breakplace")) {
            return false;
        }

        Land land = board.getLandAt(e.getBlock().getLocation());
        if (!land.hasPermission(player)) {

            String locale = e instanceof BlockBreakEvent ? "listenerBlockBreakBlocked" : "listenerBlockPlaceBlocked";
            Message.create(locale, hml)
                    .addPlaceholder("%OWNER%", land.getOwnerName())
                    .send(player);

            return true;
        }

        // randomly send message on block place if player has no land, remind to buy land
        if (!land.hasOwner()
                && Math.random() < landNotProtectedMessagePercentage
                && e instanceof BlockPlaceEvent
                && board.getLands(player).isEmpty()) {

            Message.create("listenerUnprotectedLandMessage", hml).send(player);
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onWaterAndLavaFlow(BlockFromToEvent e) {

        Chunk from = e.getBlock().getLocation().getChunk();
        Chunk to = e.getToBlock().getLocation().getChunk();
        if (!from.equals(to)) {

            Land landFrom = board.getLandAt(from);
            Land landTo = board.getLandAt(to);
            e.setCancelled(landTo.hasOwner() && !landTo.isAbandoned() && hasDifferentOwner(landTo, landFrom));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEntityEvent e) {

        Player player = e.getPlayer();
        Entity entity = e.getRightClicked();
        Land land = board.getLandAt(entity.getLocation());

        // block access to storage/hopper minecarts
        if (!e.isCancelled()
                && !land.hasPermission(player)
                && (entity instanceof HopperMinecart
                || entity instanceof StorageMinecart)) {

            Message.create("listenerNoInteract", hml)
                    .addPlaceholder("%OWNER%", land.getOwnerName())
                    .send(player);

            e.setCancelled(true);

        } else if (e.isCancelled() // uncancel cancelled interact with players in WorldGuard regions
                && entity instanceof Player
                && board.getLandAt(entity.getLocation()).hasOwner()) {
            e.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void uncancelPistonExtend(BlockPistonExtendEvent e) {
        onPistonEventCheckUncancel(e);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void uncancelPistonRetract(BlockPistonRetractEvent e) {
        onPistonEventCheckUncancel(e);
    }

    private void onPistonEventCheckUncancel(BlockPistonEvent e) {

        // only uncancel
        if (!e.isCancelled()) {
            return;
        }

        Land originLand = board.getLandAt(e.getBlock().getLocation());
        if (!originLand.hasOwner()) {
            return;
        }

        BlockFace direction = e instanceof BlockPistonRetractEvent ? e.getDirection().getOppositeFace() : e.getDirection();
        if (direction.getModX() == 0 && direction.getModZ() == 0) {
            return;
        }

        // check every block possibly pushed/pulled by the piston
        Location blockToCheck = e.getBlock().getLocation().clone();
        for (int i = 0; i < 16; i++) {

            blockToCheck.setX(blockToCheck.getX() + (double) direction.getModX());
            blockToCheck.setZ(blockToCheck.getZ() + (double) direction.getModZ());

            Block block = blockToCheck.getBlock();
            if (block.getType().equals(Material.SLIME_BLOCK)) {
                return;
            }

            Land currentLand = board.getLandAt(blockToCheck);
            if (hasDifferentOwner(currentLand, originLand)) {
                return;
            }

            // skip first block (always AIR) while retracting
            if (i == 0 && e instanceof BlockPistonRetractEvent) {
                continue;
            }

            if (block.getType().equals(Material.AIR)) {
                break;
            }
        }

        e.setCancelled(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChangeFly(PlayerChangedWorldEvent e) {

        // update fly allow on world change, as it might get overriden otherwise
        Player player = e.getPlayer();
        board.getFlyBoard().updatePlayerAllowFlight(player, board.getLandAt(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {

            Player victim = (Player) e.getEntity();
            Player attacker = Utils.getPlayerSourceFromEntity(e.getDamager());

            if (attacker != null) {
                disableFlyOnPvP(victim);
                disableFlyOnPvP(attacker);
            }
        }
    }

    private void disableFlyOnPvP(Player player) {

        FlyBoard flyBoard = board.getFlyBoard();

        if (flyBoard.isPlayerFlying(player)) {
            flyBoard.togglePlayerFlying(player);
            Message.create("listenerFlyPvPDisabled", hml)
                    .addPlaceholder("%TIME%", flyBoard.getFlyTimeLeft(player))
                    .send(player);
        }
    }

    private boolean hasDifferentOwner(Land landA, Land landB) {

        if (landA.hasOwner() != landB.hasOwner()) {
            return true;
        }

        if (landA.isServerLand() && landB.isServerLand()) {
            return false;
        }

        if (landA.hasOwner() && landB.hasOwner() && landA.isServerLand() != landB.isServerLand()) {
            return true;
        }

        return landA.hasOwner() && !landA.getOwner().equals(landB.getOwner());
    }

    private void setLastKnownChunk(Player player) {
        lastKnownChunk.put(player, player.getLocation().getChunk());
    }

    @Override
    public void loadConfig() {

        this.blockedCmds = new HashSet<>();
        List<String> blockedCmdList = hml.getConfig().getStringList("blockedCmds");
        if (blockedCmdList != null && !blockedCmdList.isEmpty()) {

            // run delayed, to ensure that every plugin is loaded yet
            scheduler.runTask(hml, () -> {
                for (String cmd : blockedCmdList) {

                    StringArgumentSeparator cmdParsed = new StringArgumentSeparator(cmd.toLowerCase(), ' ');
                    Command command = server.getPluginCommand(cmdParsed.getArgument(0));

                    if (command != null) {

                        Set<String> commandSet = new HashSet<>();
                        commandSet.add(command.getName());
                        commandSet.addAll(command.getAliases());

                        if (cmdParsed.meetsLength(2)) {
                            Set<String> commandSetWithArguments = new HashSet<>();
                            for (String s : commandSet) {
                                commandSetWithArguments.add(s + ' ' + cmdParsed.getConcatenatedString(1));
                            }
                            commandSet = commandSetWithArguments;
                        }

                        this.blockedCmds.addAll(commandSet);

                    } else {
                        hml.getLogger().warning("Command " + cmd + " was not found, skipping");
                    }
                }
            });
        }

        this.landNotProtectedMessagePercentage = hml.getConfig().getDouble("landNotProtectedMessagePercentage", 0.01d);

        if (lastKnownChunk.isEmpty()) {
            server.getOnlinePlayers().forEach(this::setLastKnownChunk);
        }
    }
}
