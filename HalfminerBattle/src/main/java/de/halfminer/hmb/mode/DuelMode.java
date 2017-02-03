package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hmb.mode.duel.DuelQueue;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Implementing duels between two players
 */
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
            MessageBuilder.create(hmb, "notAPlayer").sendMessage(sender);
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("hmb.mode.duel.use")) {
            MessageBuilder.create(hmb, "noPermission", HalfminerBattle.PREFIX).sendMessage(sender);
            return true;
        }

        if (am.getArenasFromType(GameModeType.DUEL).size() == 0) {
            MessageBuilder.create(hmb, "modeGlobalGamemodeDisabled", HalfminerBattle.PREFIX).sendMessage(sender);
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
                    MessageBuilder.create(hmb, "modeGlobalShowArenaList", HalfminerBattle.PREFIX).sendMessage(player);
                    MessageBuilder.create(hmb, am.getStringFromArenaList(am.getArenasFromType(MODE), false))
                            .setMode(MessageBuilder.Mode.DIRECT_STRING)
                            .sendMessage(player);
                    break;
                default:
                    queue.requestSend(player, hmb.getServer().getPlayer(args[0]));
            }
        } else MessageBuilder.create(hmb, "modeDuelShowHelp", HalfminerBattle.PREFIX).sendMessage(player);

        return true;
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {
        return false;
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
        broadcastWin = hmb.getConfig().getBoolean("gameMode.duel.broadcastWin", false);

        int remind = hmb.getConfig().getInt("gameMode.duel.waitingForMatchRemind", Integer.MIN_VALUE);
        if (remind < 0) {
            hmb.getConfig().set("gameMode.duel.waitingForMatchRemind", 0);
            remind = 0;
        }
        waitingForMatchRemind = remind;

        int time = hmb.getConfig().getInt("gameMode.duel.gameTime", Integer.MIN_VALUE);
        if (time < 20)
            time = 20;

        int modulo = time % 5;
        if (modulo != 0)
            time += 5 - modulo;

        if (hmb.getConfig().getInt("gameMode.duel.gameTime", Integer.MIN_VALUE) != time)
            hmb.getConfig().set("gameMode.duel.gameTime", time);
        duelTime = time + 5;

        hmb.saveConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPKickFromQueue(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {

            Player victim = (Player) e.getEntity();
            Player attacker = Utils.getDamagerFromEvent(e);

            if (pm.isInQueue(MODE, attacker))
                queue.removeFromQueue(attacker);
            if (pm.isInQueue(MODE, victim))
                queue.removeFromQueue(victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bedEnterKickFromQueue(PlayerBedEnterEvent e) {
        if (pm.isInQueue(MODE, e.getPlayer())) queue.removeFromQueue(e.getPlayer());
    }

    @EventHandler
    public void disconnectKickFromQueueOrEndDuel(PlayerQuitEvent e) {

        Player didQuit = e.getPlayer();
        if (pm.isInQueue(MODE, didQuit)) queue.removeFromQueue(didQuit);
        else if (pm.isInBattle(MODE, didQuit)) queue.gameHasFinished(didQuit, true);
    }

    @EventHandler
    public void onChatSelectArena(AsyncPlayerChatEvent e) {

        if (queue.isSelectingArena(e.getPlayer())) {
            if (am.getFreeArenasFromType(MODE).size() > 0) {
                e.setCancelled(true);

                final Player player = e.getPlayer();
                final String message = e.getMessage();
                Bukkit.getScheduler().scheduleSyncDelayedTask(HalfminerBattle.getInstance(), () -> {
                    // It may happen that between event fire and task execution the partner leaves the queue, redo select check
                    if (queue.isSelectingArena(player)) {
                        queue.arenaWasSelected(player, message);
                    }
                });
            }
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
