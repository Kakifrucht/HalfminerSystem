package de.halfminer.hmb.arena;

import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hmb.util.Util;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public class DuelArena extends AbstractKitArena {

    private static final DuelMode mode = (DuelMode) hmb.getGameMode(GameModeType.DUEL);

    private BukkitTask task;

    public DuelArena(GameModeType gameMode, String name) {
        super(gameMode, name);
    }

    public void gameStart(Player playerA, Player playerB) {

        if (!isFree())
            throw new RuntimeException("Arena " + this.getName()
                    + " currently not free while trying to start duel between " + playerA + " and " + playerB);

        addPlayers(playerA, playerB);
        clearAndStorePlayers();

        Util.sendMessage(playerA, "gameStartingCountdown", "%PLAYER%", playerB.getName(), "%ARENA%", getName());
        Util.sendMessage(playerB, "gameStartingCountdown", "%PLAYER%", playerA.getName(), "%ARENA%", getName());

        task = Bukkit.getScheduler().runTaskTimer(hmb, new Runnable() {

            private int timeLeft = mode.getDuelTime();
            private final int timeStart = timeLeft - 5;

            @Override
            public void run() {

                timeLeft -= 5;
                if (timeLeft == timeStart) { // Battle is starting, reset walkspeed and give the kit
                    preparePlayer(playerA);
                    preparePlayer(playerB);
                }
                if (timeLeft <= 15 && timeLeft > 0) {
                    Util.sendMessage(playerA, "gameTimeRunningOut", "%TIME%", Integer.toString(timeLeft));
                    Util.sendMessage(playerB, "gameTimeRunningOut", "%TIME%", Integer.toString(timeLeft));
                }
                if (timeLeft <= 0) {
                    mode.getQueue().gameHasFinished(playerA, false);
                    return;
                }
                if (timeLeft <= -1) { //just to safeguard from having alot of tasks that do not cancel and throw exceptions
                    task.cancel();
                }
            }

            private void preparePlayer(Player player) {
                player.setWalkSpeed(0.2F);
                Util.sendMessage(player, "gameStarting");
                equipPlayers();
            }

        }, 100L, 100L);
    }

    /**
     * Called once a game ends. Will issue restoring of players and resetting.
     * This happens due to death of a player (duel win), time running out, reloading of plugin
     * or logging out.
     *
     * @param loser     player that caused the game end
     * @param hasWinner true if a duel has a winner (logout or death), false if not (reload, time ran out)
     */
    public void gameEnd(Player loser, boolean hasWinner) {

        task.cancel();
        Player winner = loser.equals(playersInArena.get(0)) ? playersInArena.get(0) : playersInArena.get(1);

        restorePlayers();

        // Messaging and broadcasting
        Util.sendMessage(winner, hasWinner ? "gameWon" : "gameTied", "%PLAYER%", loser.getName());
        Util.sendMessage(loser, hasWinner ? "gameLost" : "gameTied", "%PLAYER%", winner.getName());
        if (hasWinner && mode.doWinBroadcast())
            Util.broadcastMessage("gameBroadcast", new Player[]{winner, loser}, "%WINNER%", winner.getName(), "%LOSER%", loser.getName(), "%ARENA%", name);
        playersInArena.clear();
    }

    @Override
    public boolean isFree() {
        return super.isFree() && playersInArena.size() == 0;
    }
}
