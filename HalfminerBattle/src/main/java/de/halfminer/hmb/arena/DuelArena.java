package de.halfminer.hmb.arena;

import de.halfminer.hmb.HalfminerBattle;
import de.halfminer.hmb.arena.abs.AbstractKitArena;
import de.halfminer.hmb.enums.GameModeType;
import de.halfminer.hmb.mode.DuelMode;
import de.halfminer.hms.util.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

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

        MessageBuilder.create(hmb, "gameStartingCountdown", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", playerB.getName())
                .addPlaceholderReplace("%ARENA%", getName())
                .sendMessage(playerA);
        MessageBuilder.create(hmb, "gameStartingCountdown", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", playerA.getName())
                .addPlaceholderReplace("%ARENA%", getName())
                .sendMessage(playerB);

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
                    MessageBuilder.create(hmb, "gameTimeRunningOut", HalfminerBattle.PREFIX)
                            .addPlaceholderReplace("%TIME%", Integer.toString(timeLeft))
                            .sendMessage(playerA, playerB);
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
                MessageBuilder.create(hmb, "gameStarting", HalfminerBattle.PREFIX).sendMessage(player);
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
        MessageBuilder.create(hmb, hasWinner ? "gameWon" : "gameTied", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", loser.getName())
                .sendMessage(winner);
        MessageBuilder.create(hmb, hasWinner ? "gameLost" : "gameTied", HalfminerBattle.PREFIX)
                .addPlaceholderReplace("%PLAYER%", winner.getName())
                .sendMessage(loser);

        if (hasWinner && mode.doWinBroadcast()) {
            Collection<? extends Player> sendTo = hmb.getServer().getOnlinePlayers();
            sendTo.remove(winner);
            sendTo.remove(loser);
            MessageBuilder.create(hmb, "gameBroadcast", HalfminerBattle.PREFIX)
                    .addPlaceholderReplace("%WINNER%", winner.getName())
                    .addPlaceholderReplace("%LOSER%", loser.getName())
                    .addPlaceholderReplace("%ARENA%", name)
                    .broadcastMessage(sendTo, false, "");

        }
        playersInArena.clear();
    }

    @Override
    public boolean isFree() {
        return super.isFree() && playersInArena.size() == 0;
    }
}
