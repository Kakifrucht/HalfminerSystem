package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hmb.mode.duel.DuelQueue;
import de.halfminer.hmb.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

public class DuelMode extends AbstractMode {

    private static final GameModeType MODE = GameModeType.DUEL;

    private final DuelQueue queue = new DuelQueue(this);
    private boolean broadcastWin;
    private int waitingForMatchRemind;
    private int duelTime;

    public DuelQueue getQueue() {
        return queue;
    }

    public boolean doWinBroadcast() {
        return broadcastWin;
    }

    public int getWaitingForMatchRemind() {
        return waitingForMatchRemind;
    }

    public int getDuelTime() {
        return duelTime;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed ingame");
            return false;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("hmb.mode.duel.use")) {
            Util.sendMessage(player, "noPermission");
            return true;
        }

        if (am.getArenasFromType(GameModeType.DUEL).size() == 0) {
            //TODO set message to gamemode specific disabled
            Util.sendMessage(player, "pluginDisabled");
            return true;
        }

        if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "match":
                    queue.matchPlayer(player);
                    break;
                case "leave":
                    queue.removeFromQueue(player);
                    break;
                case "list":
                    Util.sendMessage(player, "showArenaList");
                    player.sendMessage(Util.getStringFromArenaList(am.getArenasFromType(MODE), false));
                    break;
                default:
                    queue.requestSend(player, hmb.getServer().getPlayer(args[0]));
            }
        } else Util.sendMessage(player, "help");

        return true;
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed ingame");
            return true;
        }

        if (args.length < 3) {
            Util.sendMessage(sender, "adminHelp");
            return true;
        }

        Player player = (Player) sender;

        if (player.hasPermission("hmb.admin")) {

            String arg = args[1].toLowerCase();
            switch (arg) {
                case "create":
                    if (am.addArena(GameModeType.DUEL, args[2], player.getLocation()))
                        Util.sendMessage(player, "adminCreate", "%ARENA%", args[2]);
                    else Util.sendMessage(player, "adminCreateFailed", "%ARENA%", args[2]);
                    break;
                case "remove":
                    if (am.delArena(GameModeType.DUEL, args[2]))
                        Util.sendMessage(player, "adminRemove", "%ARENA%", args[2]);
                    else
                        Util.sendMessage(player, "adminArenaDoesntExist", "%ARENA%", args[2]);
                    break;
                case "spawna":
                case "spawnb":
                    if (am.setSpawn(GameModeType.DUEL, args[2], player.getLocation(),
                            arg.equalsIgnoreCase("spawna") ? 0 : 1))
                        Util.sendMessage(player, "adminSetSpawn", "%ARENA%", args[2]);
                    else
                        Util.sendMessage(player, "adminArenaDoesntExist", "%ARENA%", args[1]);
                    break;
                case "setkit":
                    if (am.setKit(GameModeType.DUEL, args[2], player.getInventory()))
                        Util.sendMessage(player, "adminSetKit", "%ARENA%", args[2]);
                    else
                        Util.sendMessage(player, "adminArenaDoesntExist", "%ARENA%", args[2]);
                    break;
                default:
                    Util.sendMessage(player, "adminHelp");
            }

        } else Util.sendMessage(sender, "noPermission");

        return true;
    }

    @Override
    public void onPluginDisable() {
        hmb.getServer().getOnlinePlayers()
                .stream()
                .filter(player -> pm.isInBattle(MODE, player))
                .forEach(p -> queue.gameHasFinished(p, false));
    }

    @Override
    public void onConfigReload() {
        broadcastWin = hmb.getConfig().getBoolean("doWinBroadcast", false);

        int remind = hmb.getConfig().getInt("waitingForMatchRemind", Integer.MIN_VALUE);
        if (remind < 0) {
            hmb.getConfig().set("waitingForMatchRemind", 0);
            remind = 0;
        }
        waitingForMatchRemind = remind;

        int time = hmb.getConfig().getInt("duelTime", Integer.MIN_VALUE);
        if (time < 20)
            time = 20;

        int modulo = time % 5;
        if (modulo != 0)
            time += 5 - modulo;

        if (hmb.getConfig().getInt("duelTime", Integer.MIN_VALUE) != time)
            hmb.getConfig().set("duelTime", time);
        duelTime = time + 5;

        hmb.saveConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPKickFromQueue(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && e.getEntity() instanceof Player) {
            Player att = (Player) e.getDamager();
            Player def = (Player) e.getEntity();
            if (pm.isInQueue(MODE, att)) queue.removeFromQueue(att);
            if (pm.isInQueue(MODE, def)) queue.removeFromQueue(def);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bedEnterKickFromQueue(PlayerBedEnterEvent e) {
        if (pm.isInQueue(MODE, e.getPlayer())) queue.removeFromQueue(e.getPlayer());
    }

    @EventHandler
    public void playerQuitRemoveQueueOrEndDuel(PlayerQuitEvent e) {

        Player didQuit = e.getPlayer();
        if (pm.isInQueue(MODE, didQuit)) queue.removeFromQueue(didQuit);
        else if (pm.isInBattle(MODE, didQuit)) queue.gameHasFinished(didQuit, true);
    }

    @EventHandler
    public void onChatSelectArena(AsyncPlayerChatEvent e) {

        if (queue.isSelectingArena(e.getPlayer())) {
            e.setCancelled(true);

            final Player player = e.getPlayer();
            final String message = e.getMessage();
            Bukkit.getScheduler().scheduleSyncDelayedTask(HalfminerBattle.getInstance(), () -> {
                // It may happen that between event fire and task execution the partner leaves the queue, redo select check
                if (queue.isSelectingArena(player)) queue.arenaWasSelected(player, message);
            });
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void playerMoveDisableDuringCountdown(PlayerMoveEvent e) {

        if (e.getPlayer().getWalkSpeed() == 0.0f && pm.isInBattle(e.getPlayer())) {
            e.getPlayer().teleport(e.getFrom());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeathEndDuel(PlayerDeathEvent e) {

        Player died = e.getEntity().getPlayer();
        if (pm.isInQueue(died)) queue.removeFromQueue(died);
        else if (pm.isInBattle(died)) queue.gameHasFinished(died, true);
    }
}
