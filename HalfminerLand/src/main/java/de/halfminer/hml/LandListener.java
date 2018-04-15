package de.halfminer.hml;

import de.halfminer.hml.land.Board;
import de.halfminer.hml.land.Land;
import de.halfminer.hms.manageable.Reloadable;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Pair;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.List;

public class LandListener extends LandClass implements Listener, Reloadable {

    private final Board board;
    private final WorldGuardHelper wgh;

    private List<Command> blockedCmds;


    LandListener(Board board, WorldGuardHelper wgh) {
        this.board = board;
        this.wgh = wgh;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        onMove(e);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {

        Chunk fromChunk = e.getFrom().getChunk();
        Chunk newChunk = e.getTo().getChunk();
        if (!fromChunk.equals(newChunk)) {

            Pair<Land, Land> previousAndNewLandPair = board.updatePlayerLocation(e.getPlayer(), fromChunk, newChunk);
            Land previous = previousAndNewLandPair.getLeft();
            Land newLand = previousAndNewLandPair.getRight();

            String pvpMessageInTitle = " ";
            String ownerMessageInTitle = " ";

            boolean currentIsPvP = wgh.isPvPEnabled(e.getTo());
            boolean pvpStateChanged = wgh.isPvPEnabled(e.getFrom()) ^ currentIsPvP;

            if (pvpStateChanged) {
                pvpMessageInTitle = MessageBuilder.returnMessage("listenerPvP" + (currentIsPvP ? "On" : "Off"), hml, false);
            }

            if (!newLand.hasOwner() && previous.hasOwner()) {
                ownerMessageInTitle = MessageBuilder.returnMessage("listenerOwnerFree", hml, false);

            } else if (newLand.hasOwner() && hasDifferentOwner(newLand, previous)) {

                ownerMessageInTitle = MessageBuilder.create("listenerOwnerOwned", hml)
                        .togglePrefix()
                        .addPlaceholderReplace("%OWNER%", newLand.getOwner().getName())
                        .returnMessage();
            }

            if (pvpMessageInTitle.length() > 1 || ownerMessageInTitle.length() > 1) {

                hms.getTitlesHandler().sendTitle(e.getPlayer(), MessageBuilder.create("%PVPLINE%\n%OWNERLINE%", hml)
                        .setDirectString()
                        .addPlaceholderReplace("%PVPLINE%", pvpMessageInTitle)
                        .addPlaceholderReplace("%OWNERLINE%", ownerMessageInTitle)
                        .returnMessage(), 0, 40, 10);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        board.updatePlayerLocation(e.getPlayer(), null, e.getPlayer().getLocation().getChunk());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        board.removePlayer(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {

        if (e.getPlayer().hasPermission("hml.bypass.cmd")) {
            return;
        }

        // check for blocked cmds on land
        Land currentLand = board.getLandAt(e.getPlayer());
        if (!currentLand.hasPermission(e.getPlayer())) {

            String command = e.getMessage().substring(1);
            int indexOf = command.indexOf(' ');
            if (indexOf > 0) {
                command = command.substring(0, indexOf);
            }

            for (Command blockedCmd : blockedCmds) {

                if (blockedCmd.getName().startsWith(command) || blockedCmd.getAliases().contains(command)) {
                    e.setCancelled(true);
                    MessageBuilder.create("listenerCmdBlocked", hml)
                            .addPlaceholderReplace("%OWNER%", currentLand.getOwner().getName())
                            .sendMessage(e.getPlayer());
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
            MessageBuilder.create(locale, hml)
                    .addPlaceholderReplace("%OWNER%", land.getOwner().getName())
                    .sendMessage(player);

            return true;
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
            e.setCancelled(landTo.hasOwner() && hasDifferentOwner(landTo, landFrom));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void uncancelPlayerInteract(PlayerInteractEntityEvent e) {
        if (e.isCancelled()
                && e.getRightClicked() instanceof Player
                && board.getLandAt(e.getRightClicked().getLocation()).hasOwner()) {
            e.setCancelled(false);
        }
    }

    private boolean hasDifferentOwner(Land landA, Land landB) {
        return (landA.hasOwner() ^ landB.hasOwner())
                || landA.hasOwner() && !landA.getOwner().equals(landB.getOwner());
    }

    @Override
    public void loadConfig() {

        this.blockedCmds = new ArrayList<>();
        List<String> blockedCmdList = hml.getConfig().getStringList("blockedCmds");
        if (blockedCmdList != null) {
            for (String cmd : blockedCmdList) {
                blockedCmds.add(server.getPluginCommand(cmd));
            }
        }
    }
}
