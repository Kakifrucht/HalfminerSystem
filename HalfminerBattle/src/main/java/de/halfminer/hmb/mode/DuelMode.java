package de.halfminer.hmb.mode;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.abs.AbstractMode;
import de.halfminer.hmb.mode.duel.DuelQueue;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Implementing duels between two players
 */
@SuppressWarnings("unused")
public class DuelMode extends AbstractMode {

    private static final GameModeType MODE = GameModeType.DUEL;

    private final DuelQueue queue = new DuelQueue(this);
    private boolean broadcastWin;
    private int waitingForMatchRemind;
    private int duelTime;

    public DuelQueue getQueue() {
        return queue;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        if (am.getArenasFromType(GameModeType.DUEL).size() == 0) {
            MessageBuilder.create(hmb, "modeGlobalGamemodeDisabled", HalfminerBattle.PREFIX).sendMessage(sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            sendArenaList(sender);
            return true;
        }

        Player player = (Player) sender;
        if (!sender.hasPermission("hmb.mode.duel.use")) {
            MessageBuilder.create(hmb, "noPermission", HalfminerBattle.PREFIX).sendMessage(sender);
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "match":
                    queue.matchPlayer(player);
                    break;
                case "leave":
                    queue.removeFromQueue(player);
                    break;
                case "list":
                    sendArenaList(sender);
                    break;
                // hidden command that will be executed on arena selection click
                case "choose":
                    if (args.length > 1 && queue.arenaWasSelected(player, args[1]))
                        break;
                default:
                    queue.requestSend(player, hmb.getServer().getPlayer(args[0]));
            }
        } else MessageBuilder.create(hmb, "modeDuelShowHelp", HalfminerBattle.PREFIX).sendMessage(sender);

        return true;
    }

    private void sendArenaList(CommandSender sender) {
        MessageBuilder.create(hmb, "modeGlobalShowArenaList", HalfminerBattle.PREFIX).sendMessage(sender);
        MessageBuilder.create(hmb, am.getStringFromGameMode(MODE))
                .setMode(MessageBuilder.Mode.DIRECT_STRING)
                .sendMessage(sender);
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
                .forEach(p -> queue.gameHasFinished(p, false, false));
    }

    @Override
    public void onConfigReload() {

        broadcastWin = hmb.getConfig().getBoolean("gameMode.duel.broadcastWin", false);

        waitingForMatchRemind = hmb.getConfig().getInt("gameMode.duel.waitingForMatchRemind", Integer.MIN_VALUE);
        if (waitingForMatchRemind < 0) {
            waitingForMatchRemind = 0;
            hmb.getConfig().set("gameMode.duel.waitingForMatchRemind", 0);
            hmb.saveConfig();
        }

        duelTime = hmb.getConfig().getInt("gameMode.duel.gameTime", Integer.MIN_VALUE);
        if (duelTime < 20) {
            duelTime = 20;
            hmb.getConfig().set("gameMode.duel.gameTime", 20);
            hmb.saveConfig();
        }
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvPKickFromQueue(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player) {

            Player victim = (Player) e.getEntity();
            Player attacker = Utils.getDamagerFromEvent(e);

            if (attacker == null)
                return;
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
        else if (pm.isInBattle(MODE, didQuit)) queue.gameHasFinished(didQuit, true, true);
    }

    @EventHandler(ignoreCancelled = true)
    public void playerMoveDisableDuringCountdown(PlayerMoveEvent e) {

        if (e.getPlayer().getWalkSpeed() == 0.0f && pm.isInBattle(MODE, e.getPlayer())) {
            e.getPlayer().teleport(e.getFrom());
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeathEndDuel(PlayerDeathEvent e) {

        Player died = e.getEntity().getPlayer();
        if (pm.isInQueue(MODE, died)) queue.removeFromQueue(died);
        else if (pm.isInBattle(MODE, died)) queue.gameHasFinished(died, true, false);
    }
}
