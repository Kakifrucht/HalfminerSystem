package de.halfminer.hmb.mode;

import de.halfminer.hmb.enums.BattleModeType;
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

    private final DuelQueue queue = new DuelQueue(this);
    private boolean broadcastWin;
    private int waitingForMatchRemind;
    private int duelTime;

    public DuelMode() {
        super(BattleModeType.DUEL);
    }

    public DuelQueue getQueue() {
        return queue;
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sendArenaList(sender);
            return true;
        }

        Player player = (Player) sender;
        if (!sender.hasPermission("hmb.mode.duel.use")) {
            MessageBuilder.create("noPermission", "Battle").sendMessage(sender);
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
                    boolean useKit = !(args.length > 1 && args[1].equalsIgnoreCase("nokit"));
                    queue.requestSend(player, server.getPlayer(args[0]), useKit);
            }
        } else MessageBuilder.create("modeDuelShowHelp", hmb).sendMessage(sender);

        return true;
    }

    private void sendArenaList(CommandSender sender) {
        MessageBuilder.create("modeGlobalShowArenaList", hmb).sendMessage(sender);
        MessageBuilder.create(am.getStringFromBattleMode(type), hmb)
                .setDirectString()
                .sendMessage(sender);
    }

    @Override
    public boolean onAdminCommand(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public void loadConfig() {

        broadcastWin = hmb.getConfig().getBoolean("battleMode.duel.broadcastWin", false);

        waitingForMatchRemind = hmb.getConfig().getInt("battleMode.duel.waitingForMatchRemind", Integer.MIN_VALUE);
        if (waitingForMatchRemind < 0) {
            waitingForMatchRemind = 0;
            hmb.getConfig().set("battleMode.duel.waitingForMatchRemind", 0);
            hmb.saveConfig();
        }

        duelTime = hmb.getConfig().getInt("battleMode.duel.gameTime", Integer.MIN_VALUE);
        if (duelTime < 20) {
            duelTime = 20;
            hmb.getConfig().set("battleMode.duel.gameTime", 20);
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
            if (pm.isInQueue(type, attacker))
                queue.removeFromQueue(attacker);
            if (pm.isInQueue(type, victim))
                queue.removeFromQueue(victim);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void bedEnterKickFromQueue(PlayerBedEnterEvent e) {
        if (pm.isInQueue(type, e.getPlayer())) queue.removeFromQueue(e.getPlayer());
    }

    @EventHandler
    public void disconnectKickFromQueueOrEndDuel(PlayerQuitEvent e) {

        Player didQuit = e.getPlayer();
        if (pm.isInQueue(type, didQuit)) queue.removeFromQueue(didQuit);
        else if (pm.isInBattle(type, didQuit)) queue.gameHasFinished(didQuit, true, true);
    }

    @EventHandler(ignoreCancelled = true)
    public void playerMoveDisableDuringCountdown(PlayerMoveEvent e) {

        if (e.getPlayer().getWalkSpeed() == 0.0f && pm.isInBattle(type, e.getPlayer())) {
            e.getPlayer().teleport(e.getFrom());
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDeathEndDuel(PlayerDeathEvent e) {

        Player died = e.getEntity();
        if (pm.isInQueue(type, died)) queue.removeFromQueue(died);
        else if (pm.isInBattle(type, died)) queue.gameHasFinished(died, true, false);
    }
}
