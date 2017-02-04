package de.halfminer.hmb.mode.duel;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.DuelArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.enums.HandlerType;
import de.halfminer.hms.handlers.HanTitles;
import de.halfminer.hms.util.MessageBuilder;
import de.halfminer.hms.util.NMSUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DuelQueue {

    private static final GameModeType MODE = GameModeType.DUEL;

    private static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    private static final PlayerManager pm = hmb.getPlayerManager();
    private static final ArenaManager am = hmb.getArenaManager();

    private static final HanTitles titles = (HanTitles) HalfminerSystem.getInstance().getHandler(HandlerType.TITLES);

    private final DuelMode duelMode;
    private final List<Player> isSelectingArena = new LinkedList<>();
    private Player waitingForMatch = null;
    private BukkitTask waitingForMatchTask;

    public DuelQueue(DuelMode duelMode) {
        this.duelMode = duelMode;
    }

    /**
     * Called after a player uses the command /duel match.
     * Puts the player into a queue until another player uses the /duel match command,
     * somebody duel requests this player, or matches the player if another player is already waiting.
     * It also sends broadcasts after <i>gameMode.duel.waitingForMatchRemind</i> setting
     * seconds if he is still waiting then
     *
     * @param toMatch player that wants to be matched
     */
    public void matchPlayer(final Player toMatch) {

        if (pm.hasQueueCooldown(toMatch)) {
            MessageBuilder.create(hmb, "modeGlobalQueueCooldown", HalfminerBattle.PREFIX).sendMessage(toMatch);
            return;
        }

        if (pm.isNotIdle(toMatch)) {
            MessageBuilder.create(hmb, "modeGlobalAlreadyInQueue", HalfminerBattle.PREFIX).sendMessage(toMatch);
            return;
        }

        if (waitingForMatch == null) {
            waitingForMatch = toMatch;
            pm.addToQueue(MODE, toMatch);
            MessageBuilder.create(hmb, "modeGlobalAddedToQueue", HalfminerBattle.PREFIX).sendMessage(toMatch);

            int time;
            if ((time = duelMode.getWaitingForMatchRemind()) > 0) {

                waitingForMatchTask = Bukkit.getScheduler().runTaskLater(hmb, () -> {

                    List<Player> sendTo = hmb.getServer().getOnlinePlayers()
                            .stream()
                            .filter(o -> !waitingForMatch.equals(o))
                            .collect(Collectors.toList());

                    MessageBuilder.create(hmb, "modeDuelPlayerWaitingForMatch", HalfminerBattle.PREFIX)
                            .addPlaceholderReplace("%PLAYER%", toMatch.getName())
                            .broadcastMessage(sendTo, false, "");
                }, time * 20);
            }
        } else {
            playersMatched(waitingForMatch, toMatch);
            clearWaitingForMatch();
        }
    }

    private void clearWaitingForMatch() {
        waitingForMatch = null;
        waitingForMatchTask.cancel();
    }

    /**
     * Called after a player specified another player as /duel argument.
     * This will either a) not do anything, either sender nor receiver are idle,
     * b) send a request to specified player, if receiver did not
     * send the request first, or c) accepts the request, if it was already
     * requested or sendTo is waiting for a match
     *
     * @param sender player that used command /duel playername
     * @param sendTo player that the request is being sent to or whose duel invitation is being accepted
     */
    public void requestSend(Player sender, Player sendTo) {

        if (sendTo == null || !sender.canSee(sendTo)) {
            MessageBuilder.create(hmb, "playerNotOnline", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (sender.equals(sendTo)) {
            MessageBuilder.create(hmb, "modeDuelRequestYourself", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (sendTo.hasPermission("hmb.mode.duel.exempt.request")) {
            MessageBuilder.create(hmb, "modeDuelRequestExempt", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                    .sendMessage(sender);
            return;
        }
        if (pm.hasQueueCooldown(sender)) {
            MessageBuilder.create(hmb, "modeGlobalQueueCooldown", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (hasRequestedDuelWith(sender, null)) {
            MessageBuilder.create(hmb, "modeDuelRequestAlreadyOpen", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (pm.isNotIdle(sender)) {
            MessageBuilder.create(hmb, "modeGlobalAlreadyInQueue", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        // Requestee is waiting for match
        if (sendTo.equals(waitingForMatch)) {
            MessageBuilder.create(hmb, "modeDuelRequestWasWaitingForMatch", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                    .sendMessage(sender);
            playersMatched(sendTo, sender);
            clearWaitingForMatch();
            return;
        }
        // Requestee sent a request already, match if requestee sent the request before
        if (hasRequestedDuelWith(sendTo, sender)) {
            MessageBuilder.create(hmb, "modeDuelRequestAccepted", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                    .sendMessage(sender);
            MessageBuilder.create(hmb, "modeDuelRequestWasAccepted", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%PLAYER%", sender.getName())
                    .sendMessage(sendTo);
            playersMatched(sendTo, sender);
            return;
        }
        if (pm.isNotIdle(sendTo)) {
            MessageBuilder.create(hmb, "modeDuelRequesteeNotAvailable", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                    .sendMessage(sender);
            return;
        }

        // if none apply create a new request
        MessageBuilder.create(hmb, "modeDuelRequestSent", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                .sendMessage(sender);
        MessageBuilder.create(hmb, "modeDuelRequest", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", sender.getName())
                .sendMessage(sendTo);
        pm.setBattlePartners(sender, sendTo);
        pm.addToQueue(MODE, sender);
    }

    /**
     * Removes a player completely from any queue, resetting his battle state, clearing send game invites
     * and removing him from the duel match. This will also work during arena selection and will remove
     * the duel partner, if applicable.
     *
     * @param toRemove player and players partner that will be removed from queue
     */
    public void removeFromQueue(Player toRemove) {

        if (!pm.isInQueue(toRemove)) {
            MessageBuilder.create(hmb, "modeDuelNotInQueue", HalfminerBattle.PREFIX).sendMessage(toRemove);
            return;
        }

        Player partner = pm.getFirstPartner(toRemove);

        if (partner != null) {

            isSelectingArena.remove(partner);
            if (hasRequestedDuelWith(toRemove, partner)) {
                MessageBuilder.create(hmb, "modeDuelRequestCancel", HalfminerBattle.PREFIX).sendMessage(toRemove);
                if (partner.isOnline()) {
                    MessageBuilder.create(hmb, "modeDuelRequestCancelled", HalfminerBattle.PREFIX)
                            .addPlaceholderReplace("%PLAYER%", toRemove.getName())
                            .sendMessage(partner);
                }
            } else {
                MessageBuilder.create(hmb, "modeGlobalLeftQueue", HalfminerBattle.PREFIX).sendMessage(toRemove);
                MessageBuilder.create(hmb, "modeDuelQueueRemovedNotTheCause", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%PLAYER%", toRemove.getName())
                        .sendMessage(partner);
                pm.setState(BattleState.IDLE, partner);
            }

        } else {
            clearWaitingForMatch();
            MessageBuilder.create(hmb, "modeGlobalLeftQueue", HalfminerBattle.PREFIX).sendMessage(toRemove);
        }

        pm.setState(BattleState.QUEUE_COOLDOWN, toRemove);
        isSelectingArena.remove(toRemove);
    }

    /**
     * Called once a pair of two players has been found, either by accepting a duel invite or
     * by matching up via /duel match. Order will be randomized.
     *
     * @param a the one duel party
     * @param b the other one
     */
    private void playersMatched(Player a, Player b) {

        Player playerA = a;
        Player playerB = b;
        if (new Random().nextBoolean()) {
            playerA = b;
            playerB = a;
        }

        pm.setBattlePartners(playerA, playerB);
        pm.setBattlePartners(playerB, playerA);
        pm.setState(BattleState.IN_QUEUE, playerA, playerB);
        pm.addToQueue(MODE, playerA, playerB);
        MessageBuilder titleMessage = MessageBuilder.create(hmb, "modeDuelShowPartnerTitle")
                .addPlaceholderReplace("%PLAYER%", playerB.getName());
        titles.sendTitle(playerA, titleMessage.returnMessage());
        titles.sendTitle(playerB, titleMessage.addPlaceholderReplace("%PLAYER%", playerA.getName()).returnMessage());

        isSelectingArena.add(playerA);
        showFreeArenaSelection(playerA, false);
    }

    /**
     * Sends the player who will select an arena all possible arena choices.
     * It sends a list where each free arena gets a number, the possibility to
     * select a random arena exists aswell. This selection updates when another player selects an arena
     * and when an arena becomes available. If only one arena is available no selection will be shown,
     * if none are free the players will be put into the next free arena automatically and notify them.
     *
     * @param player         player the selection will be sent to
     * @param refreshMessage if true will display message that the information has been refreshed
     *                       (only when free arena state updates, not on first send)
     */
    private void showFreeArenaSelection(Player player, boolean refreshMessage) {

        List<Arena> freeArenas = am.getFreeArenasFromType(MODE);
        Player partner = pm.getFirstPartner(player);

        if (freeArenas.size() == 0) {
            MessageBuilder.create(hmb, "modeDuelChooseArenaNoneAvailable", HalfminerBattle.PREFIX)
                    .sendMessage(player, partner);
        } else if (freeArenas.size() == 1) {
                arenaWasSelected(player, "1");
        } else {
            if (refreshMessage) {
                MessageBuilder.create(hmb, "modeDuelChooseArenaRefreshed", HalfminerBattle.PREFIX).sendMessage(player);
            } else {
                MessageBuilder.create(hmb, "modeDuelChooseArena", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%PLAYER%", partner.getName())
                        .sendMessage(player);

                MessageBuilder.create(hmb, "modeDuelPartnerChoosingArena", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%PLAYER%", player.getName())
                        .sendMessage(partner);
            }

            ComponentBuilder builder = new ComponentBuilder("");

            int index = 1;
            for (DuelArena free : freeArenas.stream().map(free -> (DuelArena) free).collect(Collectors.toList())) {
                String tooltipOnHover = MessageBuilder.create(hmb, "modeDuelChooseArenaHover")
                        .addPlaceholderReplace("%ARENA%", free.getName())
                        .returnMessage();
                builder.append(free.getName())
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel choose " + index++))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(tooltipOnHover).create()))
                        .color(ChatColor.GREEN)
                        .append("  ").reset();
            }

            builder.append(MessageBuilder.returnMessage(hmb, "randomArena"))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel choose " + index))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(MessageBuilder.returnMessage(hmb, "modeDuelChooseArenaRandom")).create()))
                    .color(ChatColor.GRAY);

            player.spigot().sendMessage(builder.create());
        }
    }

    /**
     * Called if a player that is currently selecting an arena made an input, information picked up by
     * {@link DuelMode#onCommand(CommandSender, String[])}
     *
     * @param player     player that chose arena
     * @param arenaIndex String that contains the players input
     */
    public void arenaWasSelected(Player player, String arenaIndex) {

        if (!isSelectingArena.contains(player)) return;

        int index = Integer.MIN_VALUE;
        try {
            index = Integer.parseInt(arenaIndex);
        } catch (NumberFormatException ignored) {}

        if (index == Integer.MIN_VALUE) { // If default value, invalid index given, cancel
            MessageBuilder.create(hmb, "modeDuelChooseArenaInvalid", HalfminerBattle.PREFIX).sendMessage(player);
            return;
        }

        Player playerB = pm.getFirstPartner(player);

        DuelArena selectedArena; // Determine which arena was selected
        List<Arena> freeArenas = am.getFreeArenasFromType(MODE);
        if (index <= freeArenas.size() && index > 0) {
            selectedArena = (DuelArena) freeArenas.get(index - 1);
        } else { // Random selected or invalid index
            Random rnd = new Random();
            selectedArena = (DuelArena) freeArenas.get(rnd.nextInt(freeArenas.size()));
        }

        MessageBuilder.create(hmb, "modeDuelStartingLog")
                .addPlaceholderReplace("%PLAYERA%", player.getName())
                .addPlaceholderReplace("%PLAYERB%", playerB.getName())
                .addPlaceholderReplace("%ARENA%", selectedArena.getName())
                .logMessage(Level.INFO);

        isSelectingArena.remove(player);
        pm.setState(BattleState.IN_BATTLE, player, playerB);
        pm.setArena(selectedArena, player, playerB);
        selectedArena.gameStart(player, playerB);

        // Update selection for players who are currently selecting
        for (Player playerSelecting : isSelectingArena)
            showFreeArenaSelection(playerSelecting, true);
    }

    /**
     * Called if a game has finished or if it was forced finished due to disable of plugin.
     * Method callees should generally only be events (plugin disable, playerDeath and playerQuit).
     * This will make sure that the arena resets and reverts both players states (inventory, location etc.),
     * refreshes the map selection for currently map selecting players and notifies the next waiting pair that
     * an arena is ready.
     *
     * @param playerA   player the game finished for
     * @param hasWinner if true, the given player is the loser of the match
     * @param hasLogged if true, the given player logged out
     */
    public void gameHasFinished(Player playerA, boolean hasWinner, boolean hasLogged) {

        DuelArena arena = (DuelArena) pm.getArena(playerA);
        Player winner = pm.getFirstPartner(playerA);

        // Messaging
        MessageBuilder.create(hmb, hasWinner ? "modeDuelGameWon" : "modeDuelGameTime", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", playerA.getName())
                .sendMessage(winner);
        MessageBuilder.create(hmb, hasWinner ? "modeDuelGameLost" : "modeDuelGameTime", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", winner.getName())
                .sendMessage(playerA);

        // broadcasting
        if (hasWinner) {

            // player logged out, ensure that winner gets the kill due to logout
            if (hasLogged) {
                NMSUtils.setKiller(playerA, winner);
                playerA.setHealth(0.0d);
            }

            if (duelMode.doWinBroadcast()) {
                List<Player> sendTo = hmb.getServer().getOnlinePlayers().stream()
                        .filter(obj -> !(obj.equals(winner) || obj.equals(playerA)))
                        .collect(Collectors.toList());

                MessageBuilder.create(hmb, "modeDuelWinBroadcast", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%WINNER%", winner.getName())
                        .addPlaceholderReplace("%LOSER%", playerA.getName())
                        .addPlaceholderReplace("%ARENA%", arena.getName())
                        .broadcastMessage(sendTo, false, "");
            }
        }

        // Reset arena and reset players
        arena.gameEnd();
        pm.setState(BattleState.IDLE, playerA, winner);

        for (Player player : isSelectingArena)
            showFreeArenaSelection(player, true);
    }

    private boolean hasRequestedDuelWith(Player requested, @Nullable Player with) {
        return pm.isInQueue(requested)
                && pm.getFirstPartner(requested) != null
                && (with == null || !pm.isInQueue(with));
    }
}
