package de.halfminer.hmduel.module;

import de.halfminer.hmduel.HalfminerDuel;
import de.halfminer.hmduel.util.Util;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Contains queues and manages players states (waiting for match/duel, waiting for selection/selecting, is in duel, starts the game
 */
public class ArenaQueue {

    private static final HalfminerDuel hmd = HalfminerDuel.getInstance();
    private final Map<Player, UUID> requests = new HashMap<>();
    private final LinkedList<Player> duelQueue = new LinkedList<>();
    /**
     * Map containing players in map selection phase. Key is selecting player, value matched player
     */
    private final Map<Player, Player> selectingArena = new HashMap<>();
    /**
     * Map containing players in a duel, where key is player a and value matched player b
     */
    private final Map<Player, Arena> inDuel = new HashMap<>();
    //Game start and run
    private final List<Arena> arenas;
    //Queue and waiting
    private Player waitingForMatch = null;
    private int waitingForMatchID;

    /**
     * Queue containing information about in which duel phase a player is (match phase [match feature and requests],
     * waiting for a free arena to open, currently selecting an arena or currently in fight.
     * It should run as a singleton in its environment.
     */
    public ArenaQueue() {

        this.arenas = hmd.getArenaManager().getArenas();

    }

    /**
     * Checks if a player is in a queue, so whether he is waiting for a match,
     * if he send a request to a player, if he is waiting with a player in the
     * queue, or if he is currently selecting an arena.
     *
     * @param toCheck player to check if he is in the queue
     * @return true if player is in a queue, false if not
     */
    public boolean isInQueue(Player toCheck) {

        return (waitingForMatch != null && waitingForMatch.equals(toCheck)) || requests.containsKey(toCheck) || duelQueue.contains(toCheck) || selectingArena.containsKey(toCheck) || selectingArena.values().contains(toCheck);
    }

    /**
     * Checks if a player is currently selecting an arena (check partially included in isInQueue()
     * method). This is necessary, to determine onChatEvent if the message sent is actually a selection
     * and not a standard chat message.
     *
     * @param toCheck player that will be checked
     * @return true if player is selecting arena, false if not
     */
    public boolean isSelectingArena(Player toCheck) {
        return selectingArena.containsKey(toCheck);
    }

    /**
     * Checks wether a player is in currently in a game
     *
     * @param toCheck player to check
     * @return true if player is in game, false if not
     */
    public boolean isInDuel(Player toCheck) {
        return inDuel.containsKey(toCheck);
    }

    /**
     * Method that is being called after a player uses the command /duel match.
     * Puts the player into a queue, until another player uses the /duel match command,
     * or matches the player and generates a valid pair, if a player is already waiting.
     * It also sends broadcasts after matchReminder setting seconds if he is still waiting.
     *
     * @param toMatch player that wants to be matched
     */
    public void matchPlayer(final Player toMatch) {
        if (isInQueue(toMatch)) Util.sendMessage(toMatch, "alreadyInQueue");
        else {
            if (waitingForMatch == null) {
                waitingForMatch = toMatch;
                Util.sendMessage(toMatch, "addedToQueue");
                int time;
                if ((time = hmd.getConfig().getInt("waitingForMatchRemind")) != 0 && time > 0) {
                    waitingForMatchID = Bukkit.getScheduler().runTaskLaterAsynchronously(hmd, new Runnable() {
                        @Override
                        public void run() {
                            if (toMatch.equals(waitingForMatch))
                                Util.broadcastMessage("playerWaitingForMatch", new String[]{"%PLAYER%", toMatch.getName()}, Collections.singletonList(toMatch));
                        }
                    }, time * 20).getTaskId();
                }
            } else {
                playersMatched(waitingForMatch, toMatch);
                waitingForMatch = null;
                Bukkit.getScheduler().cancelTask(waitingForMatchID);
            }
        }
    }

    /**
     * Method that is being called, after a player specified another player for a duel.
     * This will either a) not do anything, because the sender is already in a queue or the
     * receiver is already inDuel or inQueue, b) send a request to sendTo, if sendTo did not
     * send the request first, or c) accepts the request, if sendTo already requested it
     *
     * @param sender player that used command /duel playername
     * @param sendTo player that the request is being sent to or whose duel invitation is being accepted
     */
    public void requestSend(Player sender, Player sendTo) {
        if (sendTo.hasPermission("hmd.duel.exempt")) {
            Util.sendMessage(sender, "duelExempt", new String[]{"%PLAYER%", sendTo.getName()});
            return;
        }
        if (isInQueue(sender)) {
            Util.sendMessage(sender, "alreadyInQueue");
            return;
        }
        if (requests.containsKey(sender)) { //player already sent request
            Util.sendMessage(sender, "duelRequestAlreadyOpen");
            return;
        }
        if (requests.containsKey(sendTo)) { //requestee sent a request already, check if to sender of this request
            if (requests.get(sendTo).equals(sender.getUniqueId())) {
                Util.sendMessage(sender, "duelRequestAccepted", new String[]{"%PLAYER%", sendTo.getName()});
                Util.sendMessage(sendTo, "duelRequestWasAccepted", new String[]{"%PLAYER%", sender.getName()});
                playersMatched(sendTo, sender);
                return;
            } else {
                Util.sendMessage(sender, "duelRequesteeNotAvailable", new String[]{"%PLAYER%", sendTo.getName()});
                return;
            }
        }
        if (isInQueue(sendTo) || isInDuel(sendTo)) {
            Util.sendMessage(sender, "duelRequesteeNotAvailable", new String[]{"%PLAYER%", sendTo.getName()});
            return;
        }

        //if none apply create a new request
        Util.sendMessage(sender, "duelRequestSent", new String[]{"%PLAYER%", sendTo.getName()});
        Util.sendMessage(sendTo, "duelRequest", new String[]{"%PLAYER%", sender.getName()});
        requests.put(sender, sendTo.getUniqueId());
    }

    /**
     * Removes a player from a queue. Possible queues include the matchQueue, the requests list,
     * wether the player is currently selecting an arena or waiting until his partner selected it,
     * or if the player is actually in a duel queue. This can be called if a) the player issued
     * /duel leave, b) the player hit another player or got hit (did pvp), or c) the player logged out.
     * This also makes sure, that if a partner has already been determined, that both players are removed,
     * or if this player sent a request, that the requestee gets to know that the request was cancelled.
     *
     * @param toRemove player that will be removed from queue
     */
    public void removeFromQueue(Player toRemove) {
        if (!isInQueue(toRemove)) Util.sendMessage(toRemove, "notInQueue");
        else {
            if (waitingForMatch != null && waitingForMatch.equals(toRemove)) {
                waitingForMatch = null;
                Bukkit.getScheduler().cancelTask(waitingForMatchID);
                Util.sendMessage(toRemove, "leftQueue");
            } else if (requests.containsKey(toRemove)) {
                Player wasRequested = Bukkit.getPlayer(requests.get(toRemove));
                requests.remove(toRemove);
                Util.sendMessage(toRemove, "duelRequestCancel");
                if (wasRequested != null)
                    Util.sendMessage(wasRequested, "duelRequestCancelled", new String[]{"%PLAYER%", toRemove.getName()});
            } else if (selectingArena.containsKey(toRemove) || selectingArena.values().contains(toRemove)) {

                if (selectingArena.values().contains(toRemove)) {
                    Player partner = null;
                    for (Map.Entry<Player, Player> entry : selectingArena.entrySet()) {
                        if (entry.getValue().equals(toRemove)) {
                            partner = entry.getKey();
                            break;
                        }
                    }
                    Util.sendMessage(partner, "removedFromQueueNotTheCause", new String[]{"%PLAYER%", toRemove.getName()});
                    selectingArena.remove(partner);
                } else {
                    Player partner = selectingArena.get(toRemove);
                    Util.sendMessage(partner, "removedFromQueueNotTheCause", new String[]{"%PLAYER%", toRemove.getName()});
                    selectingArena.remove(toRemove);
                }
                Util.sendMessage(toRemove, "leftQueue");

            } else {
                int index = duelQueue.indexOf(toRemove);
                if (index % 2 == 0) {
                    //even
                    duelQueue.remove(index);
                    Util.sendMessage(duelQueue.get(index), "removedFromQueueNotTheCause", new String[]{"%PLAYER%", toRemove.getName()});
                    duelQueue.remove(index);
                } else {
                    //odd
                    duelQueue.remove(index);
                    Util.sendMessage(duelQueue.get(index - 1), "removedFromQueueNotTheCause", new String[]{"%PLAYER%", toRemove.getName()});
                    duelQueue.remove(index - 1);
                }
                Util.sendMessage(toRemove, "leftQueue");
            }
        }
    }

    /**
     * Called once a pair of two players has been found, either by accepting a duel invite or
     * by matching up via /duel match
     *
     * @param requester player that requested the duel or that matched first (gets to decide the arena)
     * @param accepter  second player, will be waiting until the arena has been selected
     */
    private void playersMatched(Player requester, Player accepter) {
        requests.remove(requester);

        boolean freeArenaAvailable = false;
        for (Arena arena : arenas)
            if (arena.isFree()) {
                freeArenaAvailable = true;
                break;
            }

        if (freeArenaAvailable) initArenaSelection(requester, accepter);
        else addToQueue(requester, accepter);

    }

    /**
     * Adds to players to the duel queue. This happens when no free arena is currently
     * available, either directly after matching / after duel request was accepted, or
     * if during the map selection another pair selected a arena first
     *
     * @param requester player that requested the duel / matched first
     * @param accepter  player that accepted / matched secondly
     */
    private void addToQueue(Player requester, Player accepter) {
        duelQueue.add(requester);
        duelQueue.add(accepter);
        Util.sendMessage(requester, "duelAddedToQueue", new String[]{"%PLAYER%", accepter.getName()});
        Util.sendMessage(accepter, "duelAddedToQueue", new String[]{"%PLAYER%", requester.getName()});
    }

    /**
     * Puts players into map selection mode, which makes the isInQueue() method still return true.
     * First player will be deciding the arena.
     *
     * @param player  player that will be selecting the arena (chatEvent will be cancelled due to input)
     * @param playerB player that will be waiting for selection
     */
    private void initArenaSelection(Player player, Player playerB) {
        Util.sendMessage(playerB, "partnerChoosingArena", new String[]{"%PLAYER%", player.getName()});
        selectingArena.put(player, playerB);
        showFreeArenaSelection(player, false);
    }

    /**
     * Method sending the selecting a player a possible map selection. This will only be called, if
     * an arena is actually free. It generates a list, where each free arena gets a number, the possibility to
     * select a random arena exists aswell. This selection updates when another player selects a arena
     * and when a arena becomes available. If only one arena is available no selection will be shown.
     *
     * @param player         player the selection will be sent to
     * @param refreshMessage if true, it will display that the information has been refreshed (only due to arena updates)
     */
    private void showFreeArenaSelection(Player player, boolean refreshMessage) {

        List<Arena> freeArenas = getFreeArenas();

        if (freeArenas.size() == 1) { //no possible selection, as only one arena is available
            arenaWasSelected(player, "1"); //1 being the only arena
            return;
        }

        if (refreshMessage) Util.sendMessage(player, "chooseArenaRefreshed");
        else Util.sendMessage(player, "chooseArena", new String[]{"%PLAYER%", selectingArena.get(player).getName()});

        StringBuilder sb = new StringBuilder();
        int number = 1;
        for (Arena arena : freeArenas) {
            sb.append("§a").append(number).append(": §7").append(arena.getName()).append(' ');
            number++;
        }
        sb.append("§a").append(number).append(": §7").append(hmd.getConfig().getString("localization.randomArena"));

        player.sendMessage(sb.toString());
    }

    /**
     * Called if a player that is currently selecting an arena made an input, generally only by the onChat listener
     *
     * @param player     player that chose arena
     * @param arenaIndex String that contains the players input, generating a random arena if input is invalid
     */
    public void arenaWasSelected(Player player, String arenaIndex) {

        short index = NumberUtils.toShort(arenaIndex, Short.MIN_VALUE);
        if (index == Short.MIN_VALUE) { //If default value, invalid index given, cancel
            Util.sendMessage(player, "chooseArenaInvalid");
            return;
        }

        Player playerB = selectingArena.get(player);
        selectingArena.remove(player);

        Arena selected; //determine which arena
        List<Arena> freeArenas = getFreeArenas();
        if (index <= freeArenas.size() && index > 0) {
            selected = freeArenas.get(index - 1);
        } else { //random selected or invalid index, select random arena
            Random rnd = new Random();
            selected = freeArenas.get(rnd.nextInt(freeArenas.size()));
        }

        hmd.getLogger().info("Duel starting between " + player.getName() + " and " + playerB.getName() + " in Arena " + selected.getName());
        inDuel.put(player, selected);
        inDuel.put(playerB, selected);
        selected.gameStart(player, playerB);

        if (!selectingArena.isEmpty()) { //Update selection
            for (Player playerReselect : selectingArena.keySet()) showFreeArenaSelection(playerReselect, true);
        }
    }

    /**
     * Called if a game has finished, or if it was forced finished due to disable of plugin.
     * Method callees should generally only be events (plugin disable, playerDeath and playerQuit).
     * This will make sure that the arena resets and reverts both players states (inventory, location etc.),
     * refreshes the map selection for a map selecting player and notfies the next waiting pair that
     * an arena is ready.
     *
     * @param playerA   player the game finished for
     * @param hasWinner if true, the given player argument is the loser of the match
     */
    public void gameHasFinished(Player playerA, boolean hasWinner) {

        Arena arena = inDuel.get(playerA);

        // Get the duel partner
        Player playerB = arena.getPlayerB().equals(playerA) ? arena.getPlayerA() : arena.getPlayerB();

        inDuel.remove(playerA);
        inDuel.remove(playerB);
        arena.gameEnd(playerA, hasWinner); // reset arena and reset players

        // Run this later, since the arena reopens with delay
        hmd.getServer().getScheduler().runTaskLater(hmd, new Runnable() {
            @Override
            public void run() {

                if (!selectingArena.isEmpty())
                    for (Player player : selectingArena.keySet()) showFreeArenaSelection(player, true);

                // start next game, without showing selection, since this is the only arena available
                if (!duelQueue.isEmpty()) initArenaSelection(duelQueue.pop(), duelQueue.pop());
            }
        }, 4L);
    }

    /**
     * @return List containing currently free arenas, in order
     */
    private List<Arena> getFreeArenas() {

        List<Arena> freeArenas = new ArrayList<>();

        for (Arena arena : arenas) {
            if (arena.isFree()) freeArenas.add(arena);
        }

        return freeArenas;

    }

    /**
     * Return a string representation of the queue, where green arenas are free and red ones in use
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int number = 1;
        String colorCode;
        for (Arena arena : arenas) {
            if (arena.isFree()) colorCode = "§a";
            else colorCode = "§c";
            sb.append("§7").append(number).append(": ").append(colorCode).append(arena.getName()).append(' ');
            number++;
        }

        return sb.toString();
    }

}
