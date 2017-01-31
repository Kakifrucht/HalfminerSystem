package de.halfminer.hmb.mode.duel;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.DuelArena;
import de.halfminer.hmb.arena.abs.Arena;
import de.halfminer.hmb.data.ArenaManager;
import de.halfminer.hmb.data.PlayerManager;
import de.halfminer.hmb.enums.BattleState;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class DuelQueue {

    private static final GameModeType MODE = GameModeType.DUEL;

    private static final HalfminerBattle hmb = HalfminerBattle.getInstance();
    private static final PlayerManager pm = hmb.getPlayerManager();
    private static final ArenaManager am = hmb.getArenaManager();

    private final DuelMode duelMode;
    private List<Player> isSelectingArena = new LinkedList<>();
    private Player waitingForMatch = null;
    private BukkitTask waitingForMatchTask;

    public DuelQueue(DuelMode duelMode) {
        this.duelMode = duelMode;
    }

    /**
     * Method that is being called after a player uses the command /duel match.
     * Puts the player into a queue until another player uses the /duel match command,
     * or matches the player and generates a valid pair, if a player is already waiting.
     * It also sends broadcasts after <i>gameMode.duel.waitingForMatchRemind</i> setting
     * seconds if he is still waiting then
     *
     * @param toMatch player that wants to be matched
     */
    public void matchPlayer(final Player toMatch) {

        if (pm.hasQueueCooldown(toMatch)) {
            MessageBuilder.create(hmb, "battleCooldown", HalfminerBattle.PREFIX).sendMessage(toMatch);
            return;
        }

        if (pm.isNotIdle(toMatch)) {
            MessageBuilder.create(hmb, "alreadyInQueue", HalfminerBattle.PREFIX).sendMessage(toMatch);
            return;
        }

        if (waitingForMatch == null) {
            waitingForMatch = toMatch;
            pm.setState(BattleState.IN_QUEUE, toMatch);
            MessageBuilder.create(hmb, "addedToQueue", HalfminerBattle.PREFIX).sendMessage(toMatch);
            int time;
            if ((time = duelMode.getWaitingForMatchRemind()) > 0) {
                waitingForMatchTask = Bukkit.getScheduler().runTaskLaterAsynchronously(hmb, () -> {
                    if (toMatch.equals(waitingForMatch)) {
                        MessageBuilder.create(hmb, "playerWaitingForMatch", HalfminerBattle.PREFIX)
                                .addPlaceholderReplace("%PLAYER%", toMatch.getName())
                                .broadcastMessage(false);
                    }
                }, time * 20);
            }
        } else {
            playersMatched(waitingForMatch, toMatch);
            waitingForMatch = null;
            waitingForMatchTask.cancel();
        }
    }

    /**
     * Method that is being called, after a player specified another player as /duel argument.
     * This will either a) not do anything, because the sender is already in a queue or because the
     * receiver is already in a duel or in a queue, b) send a request to specified player, if receiver did not
     * send the request first, or c) accepts the request, if it was already requested
     *
     * @param sender player that used command /duel playername
     * @param sendTo player that the request is being sent to or whose duel invitation is being accepted
     */
    public void requestSend(Player sender, Player sendTo) {

        if (sendTo == null || !sender.canSee(sendTo)) {
            MessageBuilder.create(hmb, "duelNotOnline", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (sender.equals(sendTo)) {
            MessageBuilder.create(hmb, "duelRequestYourself", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (sendTo.hasPermission("hmb.duel.exempt")) {
            MessageBuilder.create(hmb, "duelExempt", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                    .sendMessage(sender);
            return;
        }
        if (pm.hasQueueCooldown(sender)) {
            MessageBuilder.create(hmb, "battleCooldown", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (hasRequested(sender)) {
            MessageBuilder.create(hmb, "duelRequestAlreadyOpen", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (pm.isNotIdle(sender)) {
            MessageBuilder.create(hmb, "alreadyInQueue", HalfminerBattle.PREFIX).sendMessage(sender);
            return;
        }
        if (hasRequested(sendTo)) { // Requestee sent a request already, check if to sender of this request
            if (pm.getFirstPartner(sendTo).equals(sender)) {
                MessageBuilder.create(hmb, "duelRequestAccepted", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                        .sendMessage(sender);
                MessageBuilder.create(hmb, "duelRequestWasAccepted", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%PLAYER%", sender.getName())
                        .sendMessage(sendTo);
                playersMatched(sendTo, sender);
                return;
            } else {
                MessageBuilder.create(hmb, "duelRequesteeNotAvailable", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                        .sendMessage(sender);
                return;
            }
        }
        if (pm.isNotIdle(sendTo)) {
            MessageBuilder.create(hmb, "duelRequesteeNotAvailable", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                    .sendMessage(sender);
            return;
        }

        // if none apply create a new request
        MessageBuilder.create(hmb, "duelRequestSent", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", sendTo.getName())
                .sendMessage(sender);
        MessageBuilder.create(hmb, "duelRequest", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", sender.getName())
                .sendMessage(sendTo);
        pm.setBattlePartners(sender, sendTo);
        pm.setState(BattleState.IN_QUEUE, sender);
    }

    /**
     * Removes a player completely from any queue, resetting his battle state, clearing send game invites
     * and removing him from the /duel match. This will also work during arena selection and actual waiting.
     * It will also remove the players game partner, if applicable.
     *
     * @param toRemove player that will be removed from queue
     */
    public void removeFromQueue(Player toRemove) {

        if (!pm.isInQueue(toRemove)) {
            MessageBuilder.create(hmb, "notInQueue", HalfminerBattle.PREFIX).sendMessage(toRemove);
            return;
        }

        Player partner = pm.getFirstPartner(toRemove);

        if (partner != null) {

            isSelectingArena.remove(partner);
            if (hasRequested(toRemove)) {
                MessageBuilder.create(hmb, "duelRequestCancel", HalfminerBattle.PREFIX).sendMessage(toRemove);
                if (partner.isOnline()) {
                    MessageBuilder.create(hmb, "duelRequestCancelled", HalfminerBattle.PREFIX)
                            .addPlaceholderReplace("%PLAYER%", toRemove.getName())
                            .sendMessage(partner);
                }
            } else {
                MessageBuilder.create(hmb, "leftQueue", HalfminerBattle.PREFIX).sendMessage(toRemove);
                MessageBuilder.create(hmb, "removedFromQueueNotTheCause", HalfminerBattle.PREFIX)
                        .addPlaceholderReplace("%PLAYER%", toRemove.getName())
                        .sendMessage(partner);
                pm.setState(BattleState.IDLE, partner);
            }

        } else {
            waitingForMatch = null;
            waitingForMatchTask.cancel();
            MessageBuilder.create(hmb, "leftQueue", HalfminerBattle.PREFIX).sendMessage(toRemove);
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

        isSelectingArena.add(playerA);
        showFreeArenaSelection(playerA, false);
    }

    /**
     * Method sending the map selecting player a possible selection. This will only be called if
     * an arena is actually free. It generates a list, where each free arena gets a number, the possibility to
     * select a random arena exists aswell. This selection updates when another player selects an arena
     * and when an arena becomes available. If only one arena is available no selection will be shown.
     *
     * @param player         player the selection will be sent to
     * @param refreshMessage if true will display message that the information has been refreshed
     *                       (only when arena state updates), else show arena selection information
     */
    private void showFreeArenaSelection(Player player, boolean refreshMessage) {

        List<Arena> freeArenas = am.getFreeArenasFromType(MODE);

        if (freeArenas.size() == 0) {
            //TODO send message that no free arenas are available and it will add to free one automatically
        } else {

            if (freeArenas.size() == 1) {
                arenaWasSelected(player, "1");
            } else {
                if (refreshMessage) {
                    MessageBuilder.create(hmb, "chooseArenaRefreshed", HalfminerBattle.PREFIX).sendMessage(player);
                } else {
                    MessageBuilder.create(hmb, "chooseArena", HalfminerBattle.PREFIX)
                            .addPlaceholderReplace("%PLAYER%", pm.getFirstPartner(player).getName())
                            .sendMessage(player);
                }

                if (!refreshMessage) {
                    MessageBuilder.create(hmb, "partnerChoosingArena", HalfminerBattle.PREFIX)
                            .addPlaceholderReplace("%PLAYER%", player.getName())
                            .sendMessage(pm.getFirstPartner(player));
                }
                player.sendMessage(am.getStringFromArenaList(freeArenas, true));
            }
        }
    }

    /**
     * Called if a player that is currently selecting an arena made an input, information picked up by
     * {@link DuelMode#onChatSelectArena(AsyncPlayerChatEvent) chat event}
     *
     * @param player     player that chose arena
     * @param arenaIndex String that contains the players input, generating a random arena if input is invalid
     */
    public void arenaWasSelected(Player player, String arenaIndex) {

        int index = Integer.MIN_VALUE;
        try {
            index = Integer.parseInt(arenaIndex);
        } catch (NumberFormatException ignored) {}

        if (index == Integer.MIN_VALUE) { // If default value, invalid index given, cancel
            MessageBuilder.create(hmb, "chooseArenaInvalid", HalfminerBattle.PREFIX).sendMessage(player);
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

        hmb.getLogger().info("Duel starting between "
                + player.getName() + " and "
                + playerB.getName() + " in Arena "
                + selectedArena.getName());

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
     * @param hasWinner if true, the given player argument is the loser of the match
     */
    public void gameHasFinished(Player playerA, boolean hasWinner) {

        DuelArena arena = (DuelArena) pm.getArena(playerA);

        // Get the duel partner
        Player playerB = pm.getFirstPartner(playerA);

        arena.gameEnd(playerA, hasWinner); // Reset arena and reset players
        pm.setState(BattleState.IDLE, playerA, playerB);

        for (Player player : isSelectingArena)
            showFreeArenaSelection(player, true);
    }

    private boolean hasRequested(Player toCheck) {
        return pm.isInQueue(toCheck) && pm.getFirstPartner(toCheck) != null;
    }

    public boolean isSelectingArena(Player toCheck) {
        return isSelectingArena.contains(toCheck);
    }
}
