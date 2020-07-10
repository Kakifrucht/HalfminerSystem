package de.halfminer.hmb.mode.duel;

import de.halfminer.hmb.BattleClass;
import de.halfminer.hmb.arena.DuelArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.mode.abs.BattleModeType;
import de.halfminer.hmb.data.BattleState;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hms.HalfminerSystem;
import de.halfminer.hms.handler.HanTitles;
import de.halfminer.hms.util.Message;
import de.halfminer.hms.util.NMSUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class DuelQueue extends BattleClass {

    private static final BattleModeType MODE = BattleModeType.DUEL;
    private final DuelMode duelMode;

    private final Set<Player> hasRequestedNokit = new HashSet<>();
    private final List<Player> isSelectingArena = new LinkedList<>();
    private Player waitingForMatch = null;
    private BukkitTask waitingForMatchTask;


    public DuelQueue(DuelMode duelMode) {
        super(false);
        this.duelMode = duelMode;
    }

    /**
     * Called after a player uses the command /duel match.
     * Puts the player into a queue until another player uses the /duel match command,
     * somebody duel requests this player, or matches the player if another player is already waiting.
     * It also sends broadcasts after <i>battleMode.duel.waitingForMatchRemind</i> setting
     * seconds if he is still waiting then
     *
     * @param toMatch player that wants to be matched
     */
    public void matchPlayer(final Player toMatch) {

        if (pm.hasQueueCooldown(toMatch)) {
            Message.create("modeGlobalQueueCooldown", hmb).send(toMatch);
            return;
        }

        if (pm.isNotIdle(toMatch)) {
            Message.create("modeGlobalNotIdle", hmb).send(toMatch);
            return;
        }

        if (waitingForMatch == null) {
            waitingForMatch = toMatch;
            pm.addToQueue(MODE, toMatch);
            Message.create("modeDuelAddedToQueue", hmb).send(toMatch);

            int time;
            if ((time = duelMode.getWaitingForMatchRemind()) > 0) {

                waitingForMatchTask = scheduler.runTaskLater(hmb, () -> {

                    List<Player> sendTo = server.getOnlinePlayers()
                            .stream()
                            .filter(o -> !waitingForMatch.equals(o))
                            .collect(Collectors.toList());

                    Message.create("modeDuelPlayerWaitingForMatch", hmb)
                            .addPlaceholder("%PLAYER%", toMatch.getName())
                            .broadcast(sendTo, false, "");
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
    public void requestSend(Player sender, Player sendTo, boolean useKit) {

        if (sender.equals(sendTo)) {
            Message.create("modeDuelRequestYourself", hmb).send(sender);
            return;
        }

        if (sendTo == null || !sender.canSee(sendTo)) {
            Message.create("playerNotOnline", "Battle").send(sender);
            return;
        }

        if (sendTo.hasPermission("hmb.mode.duel.exempt.request")) {
            Message.create("modeDuelRequestExempt", hmb)
                    .addPlaceholder("%PLAYER%", sendTo.getName())
                    .send(sender);
            return;
        }

        if (pm.hasQueueCooldown(sender)) {
            Message.create("modeGlobalQueueCooldown", hmb).send(sender);
            return;
        }

        if (pm.isNotIdle(sender)) {
            Message.create("modeGlobalNotIdle", hmb).send(sender);
            return;
        }

        // Requestee is waiting for match
        if (sendTo.equals(waitingForMatch) && useKit) {
            Message.create("modeDuelRequestWasWaitingForMatch", hmb)
                    .addPlaceholder("%PLAYER%", sendTo.getName())
                    .send(sender);
            playersMatched(sendTo, sender);
            clearWaitingForMatch();
            return;
        }

        // Requestee sent a request already, match if requestee sent the request before
        if (hasRequestedDuelWith(sendTo, sender)) {
            if (hasRequestedNokit.contains(sendTo) == useKit) {
                Message.create(useKit ? "modeDuelRequestAcceptErrorNokit" : "modeDuelRequestAcceptError", hmb)
                        .addPlaceholder("%PLAYER%", sendTo.getName())
                        .send(sender);
                return;
            }
            Message.create("modeDuelRequestAccepted", hmb)
                    .addPlaceholder("%PLAYER%", sendTo.getName())
                    .send(sender);
            Message.create("modeDuelRequestWasAccepted", hmb)
                    .addPlaceholder("%PLAYER%", sender.getName())
                    .send(sendTo);
            playersMatched(sendTo, sender);
            return;
        }

        if (pm.isNotIdle(sendTo) && !pm.hasQueueCooldown(sendTo)) {
            Message.create("modeDuelRequesteeNotAvailable", hmb)
                    .addPlaceholder("%PLAYER%", sendTo.getName())
                    .send(sender);
            return;
        }

        // if none apply create a new request
        Message.create(useKit ? "modeDuelRequestSent" : "modeDuelRequestSentNokit", hmb)
                .addPlaceholder("%PLAYER%", sendTo.getName())
                .send(sender);
        Message.create(useKit ? "modeDuelRequested" : "modeDuelRequestedNokit", hmb)
                .addPlaceholder("%PLAYER%", sender.getName())
                .send(sendTo);

        if (!useKit) {
            hasRequestedNokit.add(sender);
        }

        pm.setBattlePartners(sender, sendTo);
        pm.addToQueue(MODE, sender);
    }

    private boolean hasRequestedDuelWith(Player requested, Player with) {
        Player wasRequested = pm.getFirstPartner(requested);
        return pm.isInQueue(BattleModeType.DUEL, requested)
                && wasRequested != null
                && wasRequested.equals(with);
    }

    /**
     * Removes a player completely from any (match/request) queue, resetting his battle state, clearing
     * sent game invites and removing him from the duel match. This will also work during arena selection
     * and will remove the duel partner, if applicable.
     *
     * @param toRemove player and players partner that will be removed from queue
     */
    public void removeFromQueue(Player toRemove) {

        if (!pm.isInQueue(BattleModeType.DUEL, toRemove)) {
            Message.create("modeDuelNotInQueue", hmb).send(toRemove);
            return;
        }

        Player partner = pm.getFirstPartner(toRemove);

        // if no partner is set, the player is waiting for a match
        if (partner != null) {

            Player partnerOfPartner = pm.getFirstPartner(partner);

            // if the partner of the partner is the player to be removed,
            // they are already matched, else notify about duel request cancel
            if (toRemove.equals(partnerOfPartner)) {
                Message.create("modeGlobalLeftQueue", hmb).send(toRemove);
                Message.create("modeDuelQueueRemovedNotTheCause", hmb)
                        .addPlaceholder("%PLAYER%", toRemove.getName())
                        .send(partner);
                pm.setState(BattleState.IDLE, partner);
                isSelectingArena.remove(partner);
                hasRequestedNokit.remove(partner);
            } else {
                Message.create("modeDuelRequestCancel", hmb).send(toRemove);
                if (partner.isOnline()) {
                    Message.create("modeDuelRequestCancelled", hmb)
                            .addPlaceholder("%PLAYER%", toRemove.getName())
                            .send(partner);
                }
            }

        } else {
            if (waitingForMatch == toRemove) {
                clearWaitingForMatch();
                Message.create("modeGlobalLeftQueue", hmb).send(toRemove);
            } else {
                // if not waiting for match, the duel requested player has already left
                Message.create("modeDuelRequestCancel", hmb).send(toRemove);
            }
        }

        pm.setState(BattleState.QUEUE_COOLDOWN, toRemove);
        isSelectingArena.remove(toRemove);
        hasRequestedNokit.remove(toRemove);
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
        pm.addToQueue(MODE, playerA, playerB);

        // send title containing name of duel partner
        HanTitles titleHandler = HalfminerSystem.getInstance().getTitlesHandler();
        Message titleMessage = Message.create("modeDuelShowPartnerTitle", hmb)
                .togglePrefix()
                .addPlaceholder("%PLAYER%", playerB.getName());
        titleHandler.sendTitle(playerA, titleMessage.returnMessage());
        titleHandler.sendTitle(playerB, titleMessage.addPlaceholder("%PLAYER%", playerA.getName()).returnMessage());

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
     * @param selector       player the selection will be sent to
     * @param refreshMessage if true will display message that the information has been refreshed
     *                       (only when free arena state updates, not on first send)
     */
    private void showFreeArenaSelection(Player selector, boolean refreshMessage) {

        List<Arena> freeArenas = am.getFreeArenasFromType(MODE);
        Player partner = pm.getFirstPartner(selector);

        switch (freeArenas.size()) {
            case 0:
                Message.create("modeDuelChooseArenaNoneAvailable", hmb)
                        .send(selector, partner);
                break;
            case 1:
                arenaWasSelected(selector, "random");
                break;
            default:
                if (refreshMessage) {
                    Message.create("modeDuelChooseArenaRefreshed", hmb).send(selector);
                } else {
                    Message.create("modeDuelChooseArena", hmb)
                            .addPlaceholder("%PLAYER%", partner.getName())
                            .send(selector);

                    Message.create("modeDuelPartnerChoosingArena", hmb)
                            .addPlaceholder("%PLAYER%", selector.getName())
                            .send(partner);
                }

                am.sendArenaSelection(selector, freeArenas, "/duel choose ", "modeDuelChooseArenaRandom", false);
                break;
        }
    }



    /**
     * Called if a player that is currently selecting an arena made an input, information picked up by
     * {@link DuelMode#onCommand(CommandSender, String[])}
     *
     * @param player     player that chose arena
     * @param arenaName  String that contains the arena name
     */
    public boolean arenaWasSelected(Player player, String arenaName) {

        if (!isSelectingArena.contains(player)) return false;

        Player playerB = pm.getFirstPartner(player);

        DuelArena selectedArena;
        if (arenaName.equalsIgnoreCase("random")) {
            List<Arena> freeArenas = am.getFreeArenasFromType(MODE);
            selectedArena = (DuelArena) freeArenas.get(new Random().nextInt(freeArenas.size()));
            Message.create("modeDuelOpponentChoseRandom", hmb)
                    .addPlaceholder("%PLAYER%", player.getName())
                    .send(playerB);
        } else {
            selectedArena = (DuelArena) am.getArena(MODE, arenaName);
        }

        if (selectedArena == null || !selectedArena.isFree()) {
            Message.create("modeDuelChooseArenaInvalid", hmb).send(player);
            return true;
        }

        boolean useKit = !hasRequestedNokit.contains(player) && !hasRequestedNokit.contains(playerB);
        selectedArena.gameStart(player, playerB, useKit);

        Message messageBuilder = Message.create(
                useKit ? "modeDuelCountdownStart" : "modeDuelCountdownStartNokit", hmb)
                .addPlaceholder("%PLAYER%", playerB.getName())
                .addPlaceholder("%ARENA%", selectedArena.getName());

        messageBuilder.send(player);
        messageBuilder.addPlaceholder("%PLAYER%", player.getName()).send(playerB);

        Message.create(useKit ? "modeDuelStartingLog" : "modeDuelStartingLogNokit", hmb)
                .addPlaceholder("%PLAYERA%", player.getName())
                .addPlaceholder("%PLAYERB%", playerB.getName())
                .addPlaceholder("%ARENA%", selectedArena.getName())
                .log(Level.INFO);

        isSelectingArena.remove(player);
        hasRequestedNokit.removeAll(Arrays.asList(player, playerB));

        // Update selection for players who are currently selecting
        isSelectingArena.forEach(p -> showFreeArenaSelection(p, true));
        return true;
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
     */
    public void gameHasFinished(Player playerA, boolean hasWinner) {

        Player winner = pm.getFirstPartner(playerA);
        DuelArena arena = (DuelArena) pm.getArena(playerA);
        arena.gameEnd();

        // Messaging
        Message.create(hasWinner ? "modeDuelGameWon" : "modeDuelGameTime", hmb)
                .addPlaceholder("%PLAYER%", playerA.getName())
                .send(winner);
        Message.create(hasWinner ? "modeDuelGameLost" : "modeDuelGameTime", hmb)
                .addPlaceholder("%PLAYER%", winner.getName())
                .send(playerA);

        if (hasWinner) {

            // ensure that winner gets the kill, when player logged out during duel
            if (!playerA.isDead()) {
                NMSUtils.setKiller(playerA, winner);
                playerA.setHealth(0.0d);
            }

            // broadcasting
            if (duelMode.doWinBroadcast()) {
                List<Player> sendTo = server.getOnlinePlayers().stream()
                        .filter(obj -> !(obj.equals(winner) || obj.equals(playerA)))
                        .collect(Collectors.toList());

                Message.create(arena.isUseKit() ? "modeDuelWinBroadcast" : "modeDuelWinBroadcastNokit", hmb)
                        .addPlaceholder("%WINNER%", winner.getName())
                        .addPlaceholder("%LOSER%", playerA.getName())
                        .addPlaceholder("%ARENA%", arena.getName())
                        .broadcast(sendTo, true, "");
            }
        } else {
            Message.create("modeDuelTieLog", hmb)
                    .addPlaceholder("%PLAYERA%", winner.getName())
                    .addPlaceholder("%PLAYERB%", playerA.getName())
                    .addPlaceholder("%ARENA%", arena.getName())
                    .log(Level.INFO);
        }

        isSelectingArena.forEach(p -> showFreeArenaSelection(p, true));
    }
}
